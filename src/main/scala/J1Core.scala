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

class J1Core(wordSize     : Int =  16,
             stackDepth   : Int = 256,
             addrWidth    : Int =  13,
             startAddress : Int =   0) extends Component {

  // I/O ports
  val io = new Bundle {

    // Signals for memory data port
    val writeEnable = out Bool
    val dataAddress = out UInt(addrWidth bits)
    val dataWrite = out Bits(wordSize bits)
    val dataRead = in Bits(wordSize bits)

    // I/O port for instruction port
    val instrAddress = out (UInt(addrWidth bits))
    val instr = in (Bits(wordSize bits))

  }.setName("")

  // Programm counter (PC)
  val pc = Reg(UInt(addrWidth bits)) init(startAddress)
  val pcN = UInt(addrWidth bits)

  // Instruction to be excuted
  val instr = io.instr

  // Data stack pointer (set to first entry, which can be abitrary)
  val dStackPtr = Reg(UInt(log2Up(stackDepth) bits)) init(stackDepth - 2)
  val dStackPtrN = UInt(log2Up(stackDepth) bits)

  // Write enable signal for data stack
  val dStackWrite = Bool

  // Return stack pointer, set to first entry (can be abitrary) s.t. the first write takes place at index 0
  val rStackPtr = Reg(UInt(log2Up(stackDepth) bits)) init(stackDepth - 1)
  val rStackPtrN = UInt(log2Up(stackDepth) bits)

  // Write enable for return stack
  val rStackWrite = Bool

  // Data and return stack (do not init, hence undefined value after startup) 
  val dStack = Mem(Bits(wordSize bits), wordCount = stackDepth)
  val rStack = Mem(Bits(wordSize bits), wordCount = stackDepth)

  // Top of stack (do not init, hence undefined value after startup)
  val dtos = Reg(Bits(wordSize bits)) init(255)
  val dtosN = Bits(wordSize bits)

  // Data stack write port
  dStack.write(enable  = dStackWrite,
               address = dStackPtrN,
               data    = dtos)

  // Next of data stack (read port)
  val dnos = dStack.readAsync(address = dStackPtr)

  // Calculate a possible value for top of return stack (check for conditional jump)
  val rtosN = Mux(instr(instr.high - 3 + 1)  === False,
                  (pc + 1).asBits.resize(wordSize),
                  dtos)

  // Return stack write port
  rStack.write(enable  = rStackWrite,
               address = rStackPtrN,
               data    = rtosN)

  // Top of return stack (read port)
  val rtos = rStack.readAsync(address = rStackPtr)

  // Instruction decoder (including ALU operations)
  switch(instr(instr.high downto (instr.high - 8) + 1)) {

    // Literal
    is(M"1-------") {dtosN := instr(instr.high - 1 downto 0).resize(wordSize)}

    // Jump instruction (do not change dtos)
    is(M"000-----") {dtosN := dtos}

    // Call instruction (do not change dtos)
    is(M"000-----") {dtosN := dtos}

    // Conditional jump (pop the 0 at dtos)
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

    // ALU operations using the rtos
    is(M"011-1011") {dtosN := rtos}

    // Set all bits of top of stack to false by default
    default {dtosN := (default -> False)}

  }

  // Write ALU result back to top of data stack
  dtos := dtosN

  // Internal condition flags
  val funcTtoN  = (instr(6 downto 4).asUInt === 1) // Copy DTOS to DNOS
  val funcTtoR  = (instr(6 downto 4).asUInt === 2) // Copy DTOS to return stack
  val funcWrite = (instr(6 downto 4).asUInt === 3) // Write to RAM
  val funcIOW   = (instr(6 downto 4).asUInt === 4) // I/O operation
  val isALU     = (instr(instr.high downto (instr.high - 3) + 1) === B"b011"); // ALU operation

  // Signals for handling external memory
  io.writeEnable := isALU && funcWrite
  io.dataAddress := dtosN(addrWidth - 1 downto 0).asUInt
  io.dataWrite   := dtosN

  // Increment for data stack pointer
  val dStackPtrInc = SInt(log2Up(stackDepth) bits)

  // Handle update of data stack
  switch(instr(instr.high downto (instr.high - 3) + 1)) {

    // Literal (push value to data stack)
    is(M"1--") {dStackWrite := True; dStackPtrInc := 1}

    // Conditional jump (pop DTOS from data stack)
    is(M"001") {dStackWrite := False; dStackPtrInc := -1}

    // ALU instruction
    is(M"011"){dStackWrite := funcTtoN | (instr(1 downto 0) === B"01")
               dStackPtrInc := instr(1 downto 0).asSInt.resize(log2Up(stackDepth))}

    // Don't change the data stack by default
    default {dStackWrite := False; dStackPtrInc := 0}

  }

  // Update the data stack pointer
  dStackPtrN := (dStackPtr.asSInt + dStackPtrInc).asUInt
  dStackPtr := dStackPtrN

  // Increment for data stack pointer
  val rStackPtrInc = SInt(log2Up(stackDepth) bits)

  // Handle the update of return stack
  switch(instr(instr.high downto (instr.high - 3) + 1)) {

    // Call instruction (push return address to stack)
    is(M"010") {rStackWrite := True; rStackPtrInc := 1}

    // Conditional jump (maybe we have to push)
    is(M"011") {rStackWrite := funcTtoR; rStackPtrInc := instr(3 downto 2).asSInt.resize(log2Up(stackDepth))}

    // Don't change the return stack by default
    default {rStackWrite := False; rStackPtrInc := 0}

  }

  // Update the return stack pointer
  rStackPtrN := (rStackPtr.asSInt + rStackPtrInc).asUInt
  rStackPtr := rStackPtrN

  // Handle the PC 
  switch(ClockDomain.current.isResetActive ## instr(instr.high downto instr.high - 3) ## dtos.orR) {

    // Check if we are in reset state
    is(M"1_---_-_-") {pcN := startAddress}

    // Check for jump, cond. jump or call instruction
    is(M"0_000_-_-",M"0_001_-_0",M"0_010_-_-") {pcN := instr(addrWidth - 1 downto 0).asUInt}

    // Check for R -> PC field of an ALU instruction
    is(M"0_011_1_-") {pcN := rtos(addrWidth - 1 downto 0).asUInt}

    // By default goto next instruction
    default {pcN := pc + 1}

  }

  // Update the PC
  pc := pcN

  // Use next PC as address of instruction memory
  io.instrAddress := pcN

}
