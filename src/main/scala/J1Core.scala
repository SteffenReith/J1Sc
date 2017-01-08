/*
 * Author: <AUTHORNAME>
 * Committer: <COMMITTERNAME>
 *
 * Create Date:    Tue Sep 20 15:07:10 CEST 2016 
 * Module Name:    J1Core - CPU core (ALU, Decoder, Stacks, etc)
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: e1772196dc059b6eab1272d060afbb2c17a8749a
 * Date: Mon Oct 31 22:40:41 2016 +0100
 */
import spinal.core._

class J1Core(cfg : J1Config) extends Component {

  // Check the generic parameters
  assert(Bool(cfg.wordSize == 16), "Warning: Only wordsize 16 was tested!", ERROR)
  assert(Bool(cfg.wordSize - 3 >= cfg.adrWidth), "Error: The width of an address is too large", FAILURE)

  // I/O ports
  val io = new Bundle {

    // Signals for memory and io port
    val memWriteMode = out Bool
    val ioWriteMode  = out Bool
    val ioReadMode   = out Bool
    val extAdr       = out UInt(cfg.adrWidth bits)
    val extToWrite   = out Bits(cfg.wordSize bits)
    val memToRead    = in Bits(cfg.wordSize bits)
    val ioToRead     = in Bits(cfg.wordSize bits)

    // Interface for the interrupt system
    val irq   = in Bool
    val intNo = in UInt(log2Up(cfg.irqConfig.numOfInterrupts) bits)

    // I/O port for instructions
    val instrAdr = out (UInt(cfg.adrWidth bits))
    val memInstr = in (Bits(cfg.wordSize bits))

  }.setName("")

  // Synchron reset
  val clrActive = ClockDomain.current.isResetActive

  // Program counter (PC)
  val pcN = UInt(cfg.adrWidth bits)
  val pc = RegNext(pcN) init(cfg.startAddress)
  val pcPlusOne = pc + 1
  val pc2x = pc << 1
  pc2x.keep()

  // Instruction to be executed (insert a call-instruction for an interrupt)
  val instr = Mux(io.irq, B"010" ## (((1 << cfg.adrWidth) - 1) - io.intNo).resize(cfg.wordSize - 3), io.memInstr)

  // Data stack pointer (set to first entry, which can be abitrary)
  val dStackPtrN = UInt(cfg.dataStackIdxWidth bits)
  val dStackPtr = RegNext(dStackPtrN) init((1 << cfg.dataStackIdxWidth) - 2)

  // Write enable signal for data stack
  val dStackWrite = Bool

  // Write enable for return stack
  val rStackWrite = Bool

  // Top of stack (do not init, hence undefined value after startup)
  val dtosN = Bits(cfg.wordSize bits)
  val dtos = RegNext(dtosN) init(0)

  // Data stack with read and write port
  val dStack = Mem(Bits(cfg.wordSize bits), wordCount = 1 << cfg.dataStackIdxWidth)
  dStack.write(enable  = dStackWrite,
               address = dStackPtrN,
               data    = dtos)
  val dnos = dStack.readAsync(address = dStackPtr, readUnderWrite = writeFirst)

  // Check for interrupt mode, because afterwards the current instruction has to be executed
  val nextPC = Mux(io.irq, pc.asBits.resize(cfg.wordSize), pcPlusOne.asBits.resize(cfg.wordSize))

  // Set next value for RTOS (check for conditional jump (implicitly for an interrupt) or T -> R ALU instruction field
  val rtosN = Mux(!instr(instr.high - 3 + 1), nextPC, dtos)

  // Return stack pointer, set to first entry (can be abitrary) s.t. the first write takes place at index 0
  val rStackPtrN = UInt(cfg.returnStackIdxWidth bits)
  val rStackPtr = RegNext(rStackPtrN) init((1 << cfg.returnStackIdxWidth) - 1)

  // Return stack with read and write port
  val rStack = Mem(Bits(cfg.wordSize bits), wordCount = 1 << cfg.returnStackIdxWidth)
  rStack.write(enable  = rStackWrite,
               address = rStackPtrN,
               data    = rtosN)
  val rtos = rStack.readAsync(address = rStackPtr, readUnderWrite = writeFirst)

  // Instruction decoder (including ALU operations)
  switch(instr(instr.high downto (instr.high - 8) + 1)) {

    // Literal instruction
    is(M"1-------") {dtosN := instr(instr.high - 1 downto 0).resize(cfg.wordSize)}

    // Jump and call instruction (do not change dtos)
    is(M"000-----", M"010-----") {dtosN := dtos}

    // Conditional jump (pop the 0 at dtos by adjusting the dstack pointer)
    is(M"001-----") {dtosN := dnos}

    // ALU operations using dtos and dnos
    is(M"011-0000") {dtosN := dtos}
    is(M"011-0001") {dtosN := dnos}

    // Arithmetic and logical operations (ALU)
    is(M"011-0010") {dtosN := (dtos.asUInt + dnos.asUInt).asBits}
    is(M"011-0011") {dtosN := dtos & dnos}
    is(M"011-0100") {dtosN := dtos | dnos}
    is(M"011-0101") {dtosN := dtos ^ dnos}
    is(M"011-0110") {dtosN := ~dtos}
    is(M"011-1001") {dtosN := dtos.rotateLeft(dnos(log2Up(cfg.wordSize) - 1 downto 0).asUInt)}
    is(M"011-1010") {dtosN := dtos.rotateRight(dnos(log2Up(cfg.wordSize) - 1 downto 0).asUInt)}

    // ALU operations using rtos
    is(M"011-1011") {dtosN := rtos}

    // Compare operations
    is(M"011-0111") {dtosN := (default -> (dtos === dnos))}
    is(M"011-1000") {dtosN := (default -> (dtos.asSInt > dnos.asSInt))}
    is(M"011-1111") {dtosN := (default -> (dtos.asUInt > dnos.asUInt))}

    // Memory read operations
    is(M"011-1100") {dtosN := io.memToRead}
    is(M"011-1101") {dtosN := io.ioToRead}

    // Misc operations
    is(M"011-1110") {dtosN := (rStackPtr.asBits ## dStackPtr.asBits).resized}

    // Set all bits of top of stack to true by default
    default {dtosN := (default -> True)}

  }

  // Internal condition flags
  val funcTtoN     = (instr(6 downto 4).asUInt === 1) // Copy DTOS to DNOS
  val funcTtoR     = (instr(6 downto 4).asUInt === 2) // Copy DTOS to return stack
  val funcWriteMem = (instr(6 downto 4).asUInt === 3) // Write to RAM
  val funcWriteIO  = (instr(6 downto 4).asUInt === 4) // I/O write operation
  val funcReadIO   = (instr(instr.high - 4 downto (instr.high - 8) + 1) === B"b1101") // I/O read operation
  val isALU        = (instr(instr.high downto (instr.high - 3) + 1) === B"b011") // ALU operation

  // Signals for handling external memory
  io.memWriteMode := !clrActive && isALU && funcWriteMem
  io.ioWriteMode := !clrActive && isALU && funcWriteIO
  io.ioReadMode := !clrActive && isALU && funcReadIO
  io.extAdr := dtosN(cfg.adrWidth - 1 downto 0).asUInt
  io.extToWrite := dnos

  // Increment for data stack pointer
  val dStackPtrInc = SInt(cfg.dataStackIdxWidth bits)

  // Handle update of data stack
  switch(instr(instr.high downto (instr.high - 3) + 1)) {

    // Literal (push value to data stack)
    is(M"1--") {dStackWrite := True; dStackPtrInc := 1}

    // Conditional jump (pop DTOS from data stack)
    is(M"001") {dStackWrite := False; dStackPtrInc := -1}

    // ALU instruction (check for a possible push of data)
    is(M"011"){dStackWrite := funcTtoN | (instr(1 downto 0) === B"01")
               dStackPtrInc := instr(1 downto 0).asSInt.resized}

    // Don't change the data stack by default
    default {dStackWrite := False; dStackPtrInc := 0}

  }

  // Update the data stack pointer
  dStackPtrN := (dStackPtr.asSInt + dStackPtrInc).asUInt

  // Increment for data stack pointer
  val rStackPtrInc = SInt(cfg.returnStackIdxWidth bits)

  // Handle the update of return stack
  switch(instr(instr.high downto (instr.high - 3) + 1)) {

    // Call instruction or interrupt (push return address to stack)
    is(M"010") {rStackWrite := True; rStackPtrInc := 1}

    // Conditional jump (maybe we have to push)
    is(M"011") {rStackWrite := funcTtoR; rStackPtrInc := instr(3 downto 2).asSInt.resized}

    // Don't change the return stack by default
    default {rStackWrite := False; rStackPtrInc := 0}

  }

  // Update the return stack pointer
  rStackPtrN := (rStackPtr.asSInt + rStackPtrInc).asUInt

  // Handle the PC (remember instr(7) is the R -> PC field)
  switch(clrActive ## instr(instr.high downto (instr.high - 3) + 1) ## instr(7) ## dtos.orR) {

    // Check if we are in reset state
    is(M"1_---_-_-") {pcN := cfg.startAddress}

    // Check for jump, cond. jump or call instruction
    is(M"0_000_-_-", M"0_001_-_0", M"0_010_-_-") {pcN := instr(cfg.adrWidth - 1 downto 0).asUInt}

    // Check for R -> PC field of an ALU instruction
    is(M"0_011_1_-") {pcN := rtos(cfg.adrWidth - 1 downto 0).asUInt}

    // By default goto next instruction
    default {pcN := pcPlusOne}

  }

  // Use next PC as address of instruction memory
  io.instrAdr := pcN

}
