/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 * Committer: Steffen Reith
 *
 * Create Date:    Tue Sep 20 15:07:10 CEST 2016 
 * Module Name:    J1Sc
 * Project Name:   J1Sc - A simple J1 implementation in Scala
 *
 * Hash: 9300651a2643c885bebe37daac50f3f3dd269f53
 * Date: Mon Oct 10 19:40:26 2016 +0200
 */
import spinal.core._
import spinal.lib._

class J1Core(wordSize            : Int = 16,
             dataStackIdxWidth   : Int =  8,
             returnStackIdxWidth : Int =  6,
             addrWidth           : Int = 13,
             startAddress        : Int =  0) extends Component {

  // Check the generic parameters
  assert(wordSize == 16, "Warning: Only wordsize 16 is tested!")

  // I/O ports
  val io = new Bundle {

    // Signals for memory and io port
    val writeMemEnable = out Bool
    val writeIOEnable = out Bool
    val extAdr = out UInt(addrWidth bits)
    val extToWrite = out Bits(wordSize bits)
    val memToRead = in Bits(wordSize bits)
    val ioToRead = in Bits(wordSize bits)

    // I/O port for instructions
    val instrAdr = out (UInt(addrWidth bits))
    val instr = in (Bits(wordSize bits))

  }.setName("")

  // Synchron reset
  val clr = ClockDomain.current.isResetActive

  // Programm counter (PC)
  val pcN = UInt(addrWidth bits)
  val pc = RegNext(pcN) init(startAddress)

  // Instruction to be excuted
  val instr = io.instr

  // Data stack pointer (set to first entry, which can be abitrary)
  val dStackPtrN = UInt(dataStackIdxWidth bits)
  val dStackPtr = RegNext(dStackPtrN) init((1 << dataStackIdxWidth) - 2)

  // Write enable signal for data stack
  val dStackWrite = Bool

  // Top of stack (do not init, hence undefined value after startup)
  val dtosN = Bits(wordSize bits)
  val dtos = RegNext(dtosN) init(0)

  // Data stack with read and write port
  val dStack = Mem(Bits(wordSize bits), wordCount = 1 << dataStackIdxWidth)
  dStack.write(enable  = dStackWrite,
               address = dStackPtrN,
               data    = dtos)
  val dnos = dStack.readAsync(address = dStackPtr, readUnderWrite = writeFirst)

  // Calculate a possible value for top of return stack (check for conditional jump)
  val rtosN = Mux(instr(instr.high - 3 + 1)  === False,
                  (pc + 1).asBits.resize(wordSize),
                  dtos)

  // Return stack pointer, set to first entry (can be abitrary) s.t. the first write takes place at index 0
  val rStackPtrN = UInt(returnStackIdxWidth bits)
  val rStackPtr = RegNext(rStackPtrN) init((1 << returnStackIdxWidth) - 1)

  // Write enable for return stack
  val rStackWrite = Bool

  // Return stack with read and write port
  val rStack = Mem(Bits(wordSize bits), wordCount = 1 << returnStackIdxWidth)
  rStack.write(enable  = rStackWrite,
               address = rStackPtrN,
               data    = rtosN)
  val rtos = rStack.readAsync(address = rStackPtr, readUnderWrite = writeFirst)

  // Instruction decoder (including ALU operations)
  switch(instr(instr.high downto (instr.high - 8) + 1)) {

    // Literal instruction
    is(M"1-------") {dtosN := instr(instr.high - 1 downto 0).resize(wordSize)}

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
    is(M"011-1001") {dtosN := dtos.rotateLeft(dnos(log2Up(wordSize) - 1 downto 0).asUInt)}
    is(M"011-1010") {dtosN := dtos.rotateRight(dnos(log2Up(wordSize) - 1 downto 0).asUInt)}

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
  val funcWriteIO  = (instr(6 downto 4).asUInt === 4) // I/O operation
  val isALU        = (instr(instr.high downto (instr.high - 3) + 1) === B"b011"); // ALU operation

  // Signals for handling external memory
  io.writeMemEnable := !clr && isALU && funcWriteMem
  io.writeIOEnable := !clr && isALU && funcWriteIO
  io.extAdr := dtosN(addrWidth - 1 downto 0).asUInt
  io.extToWrite   := dnos

  // Increment for data stack pointer
  val dStackPtrInc = SInt(dataStackIdxWidth bits)

  // Handle update of data stack
  switch(instr(instr.high downto (instr.high - 3) + 1)) {

    // Literal (push value to data stack)
    is(M"1--") {dStackWrite := True; dStackPtrInc := 1}

    // Conditional jump (pop DTOS from data stack)
    is(M"001") {dStackWrite := False; dStackPtrInc := -1}

    // ALU instruction
    is(M"011"){dStackWrite := funcTtoN | (instr(1 downto 0) === B"01")
               dStackPtrInc := instr(1 downto 0).asSInt.resize(dataStackIdxWidth)}

    // Don't change the data stack by default
    default {dStackWrite := False; dStackPtrInc := 0}

  }

  // Update the data stack pointer
  dStackPtrN := (dStackPtr.asSInt + dStackPtrInc).asUInt

  // Increment for data stack pointer
  val rStackPtrInc = SInt(returnStackIdxWidth bits)

  // Handle the update of return stack
  switch(instr(instr.high downto (instr.high - 3) + 1)) {

    // Call instruction (push return address to stack)
    is(M"010") {rStackWrite := True; rStackPtrInc := 1}

    // Conditional jump (maybe we have to push)
    is(M"011") {rStackWrite := funcTtoR; rStackPtrInc := instr(3 downto 2).asSInt.resize(returnStackIdxWidth)}

    // Don't change the return stack by default
    default {rStackWrite := False; rStackPtrInc := 0}

  }

  // Update the return stack pointer
  rStackPtrN := (rStackPtr.asSInt + rStackPtrInc).asUInt

  // Handle the PC
  switch(clr ## instr(instr.high downto instr.high - 3) ## dtos.orR) {

    // Check if we are in reset state
    is(M"1_---_-_-") {pcN := startAddress}

    // Check for jump, cond. jump or call instruction
    is(M"0_000_-_-", M"0_001_-_0", M"0_010_-_-") {pcN := instr(addrWidth - 1 downto 0).asUInt}

    // Check for R -> PC field of an ALU instruction
    is(M"0_011_1_-") {pcN := rtos(addrWidth - 1 downto 0).asUInt}

    // By default goto next instruction
    default {pcN := pc + 1}

  }

  // Use next PC as address of instruction memory
  io.instrAdr := pcN

}
