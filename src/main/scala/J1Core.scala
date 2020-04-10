/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Create Date:    Tue Sep 20 15:07:10 CEST 2016
 * Module Name:    J1Core - CPU core (ALU, Decoder, Stacks, etc)
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core.{Bits, _}

class J1Core(cfg : J1Config) extends Component {

  // Check the generic parameters
  assert(cfg.wordSize == 16, message = "ERROR: Only wordsize 16 was tested!")
  assert((cfg.wordSize - 3) >= cfg.adrWidth, message = "ERROR: The width of an address is too large")

  // Internally used signals
  val internal = new Bundle {

    // Signals for memory and io port
    val memWriteMode = out Bool
    val ioWriteMode  = out Bool
    val ioReadMode   = out Bool
    val extAdr       = out UInt(cfg.wordSize bits)
    val extToWrite   = out Bits(cfg.wordSize bits)
    val toRead       = in  Bits(cfg.wordSize bits)

    // Signal to stall the CPU
    val stall = in Bool

    // Interface for the interrupt system
    val irq    = in Bool
    val intVec = in Bits (cfg.adrWidth bits)

    // I/O port for instructions
    val nextInstrAdr = out UInt(cfg.adrWidth bits)
    val memInstr     = in Bits(cfg.wordSize bits)

  }.setName("")

  // Synchronous reset signal
  val clrActive = ClockDomain.current.isResetActive

  // The stall signal is allowed to be pruned (if we have no JTAG)
  internal.stall.allowPruning()

  // Program counter (note that the MSB is used to control dstack and rstack, hence make is one bit larger)
  val pcN       = UInt(cfg.adrWidth + 1 bits)
  val pc        = RegNextWhen(pcN, !clrActive) init(cfg.startAddress)
  val pcPlusOne = pc + 1

  // Instruction to be executed (inject a call-instruction for handling an interrupt and handle a stall)
  val stateSelect = internal.stall ## internal.irq

  // Check status and inject nop (stall mode) or jump (interrupt) when needed
  val instr = stateSelect.mux(0 -> internal.memInstr,                                   // Normal mode
                              1 -> B"b010" ## internal.intVec.resize(cfg.wordSize - 3), // Interrupt mode
                              2 -> J1Config.instrNOP(cfg.wordSize),                     // Stall mode
                              3 -> J1Config.instrNOP(cfg.wordSize))                     // Stall overrides interrupt

  // Data stack pointer (set to first entry, which can be arbitrary)
  val dStackPtrN = UInt(cfg.dataStackIdxWidth bits)
  val dStackPtr  = RegNextWhen(dStackPtrN, !internal.stall) init(0)

  // Write enable signal for data stack
  val dStackWrite = Bool

  // Write enable for return stack
  val rStackWrite = Bool

  // Top of stack and next value
  val dtosN = Bits(cfg.wordSize bits)
  val dtos  = RegNext(dtosN) init(0)

  // Data stack with read and write port
  val dStack = Mem(Bits(cfg.wordSize bits), 1 << cfg.dataStackIdxWidth)
  dStack.write(address = dStackPtrN,
               data    = dtos,
               enable  = dStackWrite & !internal.stall)
  val dnos = dStack.readAsync(address = dStackPtr, readUnderWrite = writeFirst)

  // Check for interrupt mode, because afterwards the current instruction has to be executed
  val retPC = Mux(internal.irq, pc.asBits, pcPlusOne.asBits)

  // Set next value for RTOS (check call / interrupt or T -> R ALU instruction)
  val rtosN = Mux(!instr(instr.high - 3 + 1), (retPC ## B"b0").resized, dtos)

  // Return stack pointer, set to first entry (can be arbitrary) s.t. the first write takes place at index 0
  val rStackPtrN = UInt(cfg.returnStackIdxWidth bits)
  val rStackPtr = RegNextWhen(rStackPtrN, !internal.stall) init(0)

  // Return stack with read and write port
  val rStack = Mem(Bits(cfg.wordSize bits), 1 << cfg.returnStackIdxWidth)
  rStack.write(address = rStackPtrN,
               data    = rtosN,
               enable  = rStackWrite & !internal.stall)
  val rtos = rStack.readAsync(address = rStackPtr, readUnderWrite = writeFirst)

  // Create an ALU
  val alu = J1Alu(cfg)
  val aluResult = alu(instr     = instr,
                      dtos      = dtos,
                      dnos      = dnos,
                      dStackPtr = dStackPtr,
                      rtos      = rtos,
                      toRead    = internal.toRead)

  // Instruction decoder
  switch(pc.msb ## instr(instr.high downto (instr.high - 3) + 1)) {

    // If there is a high call then push the instruction (== memory access) to the data stack
    is(M"1_---") {dtosN := instr}

    // Literal instruction (Push value)
    is(M"0_1--") {dtosN := instr(instr.high - 1 downto 0).resized}

    // Jump and call instruction (do not change dtos)
    is(M"0_000", M"0_010") {dtosN := dtos}

    // Conditional jump (pop a 0 at dtos by adjusting the dstack pointer)
    is(M"0_001") {dtosN := dnos}

    // Check for ALU operation
    is(M"0_011") {dtosN := aluResult}

    // Set all bits of top of stack to true by default
    default {dtosN := (default -> True)}

  }

  // Internal condition flags
  val funcTtoN     = instr(6 downto 4).asUInt === 1 // Copy DTOS to DNOS
  val funcTtoR     = instr(6 downto 4).asUInt === 2 // Copy DTOS to return stack
  val funcWriteMem = instr(6 downto 4).asUInt === 3 // Write to RAM
  val funcWriteIO  = instr(6 downto 4).asUInt === 4 // I/O write operation
  val funcReadIO   = instr(6 downto 4).asUInt === 5 // I/O read operation
  val isALU        = !pc.msb && (instr(instr.high downto (instr.high - 3) + 1) === B"b011") // ALU operation

  // Signals for handling external memory
  internal.memWriteMode := !clrActive && isALU && funcWriteMem
  internal.ioWriteMode  := !clrActive && isALU && funcWriteIO
  internal.ioReadMode   := !clrActive && isALU && funcReadIO
  internal.extAdr       := dtosN.asUInt
  internal.extToWrite   := dnos

  // Increment for data stack pointer
  val dStackPtrInc = SInt(cfg.dataStackIdxWidth bits)

  // Handle the update of the data stack
  switch(pc.msb ## instr(instr.high downto (instr.high - 3) + 1)) {

    // For a high call push the instruction (== memory access) and for a literal push the value to the data stack
    is(M"1_---", M"0_1--") {dStackWrite := True; dStackPtrInc := 1}

    // Conditional jump (pop DTOS from data stack)
    is(M"0_001") {dStackWrite := False; dStackPtrInc := -1}

    // ALU instruction (check for a possible push of data, ISA bug can be fixed by '| (instr(1 downto 0) === B"b01")')
    is(M"0_011"){dStackWrite := funcTtoN; dStackPtrInc := instr(1 downto 0).asSInt.resized}

    // Don't change the data stack by default
    default {dStackWrite := False; dStackPtrInc := 0}

  }

  // Update the data stack pointer
  dStackPtrN := (dStackPtr.asSInt + dStackPtrInc).asUInt

  // Increment for return stack pointer
  val rStackPtrInc = SInt(cfg.returnStackIdxWidth bits)

  // Handle the update of the return stack
  switch(pc.msb ## instr(instr.high downto (instr.high - 3) + 1)) {

    // When we do a high call (the msb of the PC is set) do a pop of return address
    is(M"1_---") {rStackWrite := False; rStackPtrInc := -1}

    // Call instruction or interrupt (push return address to stack)
    is(M"0_010") {rStackWrite := True; rStackPtrInc := 1}

    // Conditional jump (maybe we have to push)
    is(M"0_011") {rStackWrite := funcTtoR; rStackPtrInc := instr(3 downto 2).asSInt.resized}

    // Don't change the return stack by default
    default {rStackWrite := False; rStackPtrInc := 0}

  }

  // Update the return stack pointer
  rStackPtrN := (rStackPtr.asSInt + rStackPtrInc).asUInt

  // Create the PC update logic
  val pcNext = J1PCNext(cfg)
  pcN := pcNext(stall     = internal.stall,
                clrActive = clrActive,
                pc        = pc,
                pcPlusOne = pcPlusOne,
                instr     = instr,
                dtos      = dtos,
                rtos      = rtos)

  // Use next PC as address of instruction memory (do not use the MSB)
  internal.nextInstrAdr := pcN(pcN.high - 1 downto 0)

}
