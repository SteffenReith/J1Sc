/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Create Date:    Tue Sep 20 15:07:10 CEST 2016
 * Module Name:    J1Core - CPU core (ALU, Decoder, Stacks, etc)
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._

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

  // The stall signal is allowed to be pruned (if we have no JTAG)
  internal.stall.allowPruning()

  // Synchronous reset signal
  val clrActive = ClockDomain.current.isResetActive

  // Program counter (note that the MSB is used to control dstack and rstack, hence make is one bit larger)
  val pcN       = UInt(cfg.adrWidth + 1 bits)
  val pc        = RegNextWhen(pcN, !clrActive) init(cfg.startAddress)
  val pcPlusOne = pc + 1

  // Check for interrupt mode, because afterwards the current instruction has to be executed
  val returnPC = Mux(internal.irq, pc.asBits, pcPlusOne.asBits)

  // Check status and inject nop (stall mode) or call-instruction (interrupt mode) when needed
  val stateSelect = internal.stall ## internal.irq
  val instr = stateSelect.mux(B"b00" -> internal.memInstr,                                   // Normal mode
                              B"b01" -> B"b010" ## internal.intVec.resize(cfg.wordSize - 3), // Interrupt mode
                              B"b10" -> J1Config.instrNOP(cfg.wordSize),                     // Stall mode
                              B"b11" -> J1Config.instrNOP(cfg.wordSize))                     // Stall overrides interrupt

  // Write enable signals for data and return stack
  val rStackWriteEnable = Bool

  // Top of data stack and next value
  val dtosN = Bits(cfg.wordSize bits)

  // Create the Data stack
  val dStack     = J1DStack(cfg)
  val dStackInfo = dStack(internal.stall, dtosN)
  val dtos       = dStackInfo._1
  val dnos       = dStackInfo._2
  val dStackPtr  = dStackInfo._3

  // Set next value for RTOS (check call / interrupt or T -> R ALU instruction)
  val rtosN = Mux(!instr(instr.high - 3 + 1), (returnPC ## B"b0").resized, dtos)

  // Return stack pointer, set to first entry (can be arbitrary) s.t. the first write takes place at index 0
  val rStackPtrN = UInt(cfg.returnStackIdxWidth bits)
  val rStackPtr = RegNextWhen(rStackPtrN, !internal.stall) init(0)

  // Return stack with read and write port
  val rStack = Mem(Bits(cfg.wordSize bits), wordCount = (1 << cfg.returnStackIdxWidth))
  rStack.write(address = rStackPtrN,
               data    = rtosN,
               enable  = rStackWriteEnable & !internal.stall)
  val rtos = rStack.readAsync(address = rStackPtr, readUnderWrite = writeFirst)

  // Create an ALU (the AluOp is taken out of the instruction)
  val alu = J1Alu(cfg)
  val aluResult = alu(instr, dtos, dnos, dStackPtr, rtos, internal.toRead)

  // Decode instruction and calculate next top of data stack
  dtosN := J1Decoder(cfg)(pc, instr, dtos, dnos, aluResult)

  // Internal condition flags
  val funcTtoN     = instr(6 downto 4) === B"001" // Copy DTOS to DNOS
  val funcTtoR     = instr(6 downto 4) === B"010" // Copy DTOS to return stack
  val funcWriteMem = instr(6 downto 4) === B"011" // Write to RAM
  val funcWriteIO  = instr(6 downto 4) === B"100" // I/O write operation
  val funcReadIO   = instr(6 downto 4) === B"101" // I/O read operation
  val isALU        = !pc.msb && (instr(instr.high downto (instr.high - 3) + 1) === B"b011") // ALU operation

  // Control signals for external memory
  internal.memWriteMode := !clrActive && isALU && funcWriteMem
  internal.ioWriteMode  := !clrActive && isALU && funcWriteIO
  internal.ioReadMode   := !clrActive && isALU && funcReadIO
  internal.extAdr       := dtosN.asUInt
  internal.extToWrite   := dnos

  // Update the data stack
  dStack.updateDStack(pc.msb, instr, funcTtoN)

  // Increment for return stack pointer
  val rStackPtrInc = SInt(cfg.returnStackIdxWidth bits)

  // Handle the update of the return stack
  switch(pc.msb ## instr(instr.high downto (instr.high - 3) + 1)) {

    // When we do a high call (the msb of the PC is set) do a pop of return address
    is(M"1_---") {rStackWriteEnable := False; rStackPtrInc := -1}

    // Call instruction or interrupt (push return address to stack)
    is(M"0_010") {rStackWriteEnable := True; rStackPtrInc := 1}

    // Conditional jump (maybe we have to push)
    is(M"0_011") {rStackWriteEnable := funcTtoR; rStackPtrInc := instr(3 downto 2).asSInt.resized}

    // Don't change the return stack by default
    default {rStackWriteEnable := False; rStackPtrInc := 0}

  }

  // Update the return stack pointer
  rStackPtrN := (rStackPtr.asSInt + rStackPtrInc).asUInt

  // Create the PC update logic
  val j1next = J1PCNext(cfg)
  pcN := j1next(stall     = internal.stall,
                clrActive = clrActive,
                pc        = pc,
                pcPlusOne = pcPlusOne,
                instr     = instr,
                dtos      = dtos,
                rtos      = rtos)

  // Use next PC as address of instruction memory (do not use the MSB)
  internal.nextInstrAdr := pcN(pcN.high - 1 downto 0)

}
