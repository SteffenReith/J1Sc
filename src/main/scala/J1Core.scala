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
  assert(Bool(cfg.wordSize == 16), "Warning: Only wordsize 16 was tested!", ERROR)
  assert(Bool(cfg.wordSize - 3 >= cfg.adrWidth), "Error: The width of an address is too large", FAILURE)

  // Internally used signals
  val internal = new Bundle {

    // Signals for memory and io port
    val memWriteMode = out Bool
    val ioWriteMode  = out Bool
    val ioReadMode   = out Bool
    val extAdr       = out UInt(cfg.wordSize bits)
    val extToWrite   = out Bits(cfg.wordSize bits)
    val toRead       = in  Bits(cfg.wordSize bits)

    // Interface for the interrupt system
    val irq    = in Bool
    val intVec = in Bits (cfg.adrWidth bits)

    // I/O port for instructions
    val nextInstrAdr = out (UInt(cfg.adrWidth bits))
    val memInstr     = in (Bits(cfg.wordSize bits))

  }.setName("")

  // Synchron reset
  val clrActive = ClockDomain.current.isResetActive

  // Program counter (note that the MSB is used to control dstack and rstack, hence make is one bit larger)
  val pcN = UInt(cfg.adrWidth + 1 bits)
  val pc = RegNext(pcN) init(cfg.startAddress)
  val pcPlusOne = pc + 1

  // Instruction to be executed (insert a call-instruction for handling an interrupt)
  val instr = Mux(internal.irq, B"b010" ## internal.intVec.resize(cfg.wordSize - 3), internal.memInstr)

  // Data stack pointer (set to first entry, which can be abitrary)
  val dStackPtrN = UInt(cfg.dataStackIdxWidth bits)
  val dStackPtr = RegNext(dStackPtrN) init(0)

  // Write enable signal for data stack
  val dStackWrite = Bool

  // Write enable for return stack
  val rStackWrite = Bool

  // Top of stack and next value
  val dtosN = Bits(cfg.wordSize bits)
  val dtos = RegNext(dtosN) init(0)

  // Data stack with read and write port
  val dStack = Mem(Bits(cfg.wordSize bits), wordCount = 1 << (cfg.dataStackIdxWidth))
  dStack.write(enable  = dStackWrite,
               address = dStackPtrN,
               data    = dtos)
  val dnos = dStack.readAsync(address = dStackPtr, readUnderWrite = writeFirst)

  // Check for interrupt mode, because afterwards the current instruction has to be executed
  val retPC = Mux(internal.irq, pc.asBits, pcPlusOne.asBits)

  // Set next value for RTOS (check call / interrupt or T -> R ALU instruction)
  // val rtosN = Mux(!instr(instr.high - 3 + 1), (retPC(retPC.high - 1  downto 0) ## B"b0").resized, dtos)
  val rtosN = Mux(!instr(instr.high - 3 + 1), (retPC ## B"b0").resized, dtos)

  // Return stack pointer, set to first entry (can be arbitrary) s.t. the first write takes place at index 0
  val rStackPtrN = UInt(cfg.returnStackIdxWidth bits)
  val rStackPtr = RegNext(rStackPtrN) init(0)

  // Return stack with read and write port
  val rStack = Mem(Bits(cfg.wordSize bits), wordCount = (1 << cfg.returnStackIdxWidth))
  rStack.write(enable  = rStackWrite,
               address = rStackPtrN,
               data    = rtosN)
  val rtos = rStack.readAsync(address = rStackPtr, readUnderWrite = writeFirst)

  // Calculate difference (- dtos + dnos) and sign to be reused multiple times
  val difference = dnos.resize(cfg.wordSize + 1).asSInt - dtos.resize(cfg.wordSize + 1).asSInt
  val nosIsLess = (dtos.msb ^ dnos.msb) ? dnos.msb | difference.msb

  // Instruction decoder (including ALU operations)
  switch(pc.msb ## instr(instr.high downto (instr.high - 8) + 1)) {

    // Push instruction to dstack
    is(M"1_--------") {dtosN := instr}

    // Literal instruction (Push value)
    is(M"0_1-------") {dtosN := instr(instr.high - 1 downto 0).resized}

    // Jump and call instruction (do not change dtos)
    is(M"0_000-----", M"0_010-----") {dtosN := dtos}

    // Conditional jump (pop a 0 at dtos by adjusting the dstack pointer)
    is(M"0_001-----") {dtosN := dnos}

    // ALU operations using dtos and dnos
    is(M"0_011-0000") {dtosN := dtos}
    is(M"0_011-0001") {dtosN := dnos}

    // Arithmetic and logical operations (ALU)
    is(M"0_011-0010") {dtosN := (dtos.asUInt + dnos.asUInt).asBits}
    is(M"0_011-1100") {dtosN := difference.resize(cfg.wordSize).asBits}
    is(M"0_011-0011") {dtosN := dtos & dnos}
    is(M"0_011-0100") {dtosN := dtos | dnos}
    is(M"0_011-0101") {dtosN := dtos ^ dnos}
    is(M"0_011-0110") {dtosN := ~dtos}
    is(M"0_011-1001") {dtosN := dtos(dtos.high) ## dtos(dtos.high downto 1).asUInt}
    is(M"0_011-1010") {dtosN := dtos(dtos.high - 1 downto 0) ## B"b0"}

    // ALU operations using rtos
    is(M"0_011-1011") {dtosN := rtos}

    // Compare operations (equal, dtos > dnos, signed and unsigned)
    is(M"0_011-0111") {dtosN := (default -> (difference === 0))}
    is(M"0_011-1000") {dtosN := (default -> nosIsLess)}
    is(M"0_011-1111") {dtosN := (default -> difference.msb)}

    // Memory / IO read operations
    is(M"0_011-1101") {dtosN := internal.toRead}

    // Misc operations
    is(M"0_011-1110") {dtosN := dStackPtr.asBits.resized}

    // Set all bits of top of stack to true by default
    default {dtosN := (default -> True)}

  }

  // Internal condition flags
  val funcTtoN     = (instr(6 downto 4).asUInt === 1) // Copy DTOS to DNOS
  val funcTtoR     = (instr(6 downto 4).asUInt === 2) // Copy DTOS to return stack
  val funcWriteMem = (instr(6 downto 4).asUInt === 3) // Write to RAM
  val funcWriteIO  = (instr(6 downto 4).asUInt === 4) // I/O write operation
  val funcReadIO   = (instr(6 downto 4).asUInt === 5) // I/O read operation
  val isALU        = !pc.msb && (instr(instr.high downto (instr.high - 3) + 1) === B"b011") // ALU operation
  
  // Signals for handling external memory
  internal.memWriteMode := !clrActive && isALU && funcWriteMem
  internal.ioWriteMode := !clrActive && isALU && funcWriteIO
  internal.ioReadMode := !clrActive && isALU && funcReadIO
  internal.extAdr := dtosN.asUInt
  internal.extToWrite := dnos

  // Increment for data stack pointer
  val dStackPtrInc = SInt(cfg.dataStackIdxWidth bits)

  // Handle update of data stack
  switch(pc.msb ## instr(instr.high downto (instr.high - 3) + 1)) {

    // Either push instruction to stack or literal (push value to data stack)
    is(M"1_---", M"0_1--") {dStackWrite := True; dStackPtrInc := 1}

    // Conditional jump (pop DTOS from data stack)
    is(M"0_001") {dStackWrite := False; dStackPtrInc := -1}

    // ALU instruction (check for a possible push of data, ISA bug can be fixed by '| (instr(1 downto 0) === B"b01")')
    is(M"0_011"){dStackWrite  := funcTtoN; dStackPtrInc := instr(1 downto 0).asSInt.resized}

    // Don't change the data stack by default
    default {dStackWrite := False; dStackPtrInc := 0}

  }

  // Update the data stack pointer
  dStackPtrN := (dStackPtr.asSInt + dStackPtrInc).asUInt

  // Increment for data stack pointer
  val rStackPtrInc = SInt(cfg.returnStackIdxWidth bits)

  // Handle the update of return stack
  switch(pc.msb ## instr(instr.high downto (instr.high - 3) + 1)) {

    // Pseudo pop of return address when msb of PC is set
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

  // Handle the PC (remember cfg.adrWidth - 1 is the high indicator and instr(7) is the R -> PC field)
  switch(clrActive ## pc.msb ## instr(instr.high downto (instr.high - 3) + 1) ## instr(7) ## dtos.orR) {

    // Check if we are in reset state
    is(M"1_-_---_-_-") {pcN := cfg.startAddress}

    // Check for jump, call instruction or conditional jump
    is(M"0_0_000_-_-", M"0_0_010_-_-", M"0_0_001_-_0") {pcN := instr(cfg.adrWidth downto 0).asUInt}

    // Check either for a high call or R -> PC field of an ALU instruction
    is(M"0_1_---_-_-", M"0_0_011_1_-") {pcN := rtos(cfg.adrWidth + 1 downto 1).asUInt}

    // By default goto next instruction
    default {pcN := pcPlusOne}

  }

  // Use next PC as address of instruction memory (do not use the MSB)
  internal.nextInstrAdr := pcN(pcN.high - 1 downto 0)

}
