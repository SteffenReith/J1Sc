/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 * Committer: Steffen Reith
 *
 * Create Date:    Tue Sep 20 15:07:10 CEST 2016 
 * Module Name:    J1Sc
 * Project Name:   J1Sc - A simple J1 implementation in Scala
 *
 * Hash: bc807384d0d2b794942f61aab9c8f504ae031538
 * Date: Wed Sep 21 00:06:08 2016 +0200
 */
import spinal.core._
import spinal.lib._

class J1Core(wordSize     : Int =  16,
             stackDepth   : Int = 256,
             addrWidth    : Int =  13,
             startAddress : Int =   0) extends Component {

  // I/O ports
  val io = new Bundle {

    // I/O signals for memory data port
    val writeEnable = out Bool
    val dataAddress = out UInt(addrWidth bits)
    val dataWrite = out Bits(wordSize bits)
    val dataRead = in Bits(wordSize bits)

    // I/O port for instruction port
    val instrAddress = out (UInt(addrWidth bits))
    val instr = in (Bits(wordSize bits))

  }.setName("")

  // Write Enable
  // DataAddress
  // DataWrite
  io.writeEnable := False;
  io.dataAddress := 0;
  io.dataWrite := B(0, wordSize bits)

  // Data stack pointer (init to first entry)
  val dStackPtr = Reg(UInt(log2Up(stackDepth) bits)) init(0)  

  // Write enable signal for data stack
  val dStackWrite : Bool = False

  // Return stack pointer (init to first entry)
  val rStackPtr = Reg(UInt(log2Up(stackDepth) bits)) init(0)

  // Write enable for return stack
  val rStackWrite : Bool = False

  // Data and return stack (do not init, hence undefined value after startup) 
  val dStack = Mem(Bits(wordSize bits), wordCount = stackDepth)
  val rStack = Mem(Bits(wordSize bits), wordCount = stackDepth)

  // Top of stack (do not init, hence undefined value after startup)
  val dtos = Reg(Bits(wordSize bits))

  // Next of data stack (read port)
  val dnos = dStack.readAsync(address = dStackPtr);

  // Data stack write port
  val dtosN = Bits(wordSize bits)
  dStack.write(enable  = dStackWrite,
               address = dStackPtr,
               data    = dtosN)

  // Return stack write port
  val rtosN = Bits(wordSize bits)
  rStack.write(enable  = rStackWrite,
               address = rStackPtr,
               data    = rtosN)

  // Top of return stack (read port)
  val rtos = rStack.readAsync(address = rStackPtr)

  // Programm counter (PC)
  val pc = Reg(UInt(addrWidth bits)) init(startAddress)

  // Instruction to be excuted
  val instr = io.instr

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
    is(M"011-1001") {dtosN := dtos >> dnos(log2Up(wordSize - 1) downto 0).asUInt}
    is(M"011-1010") {dtosN := dtos << dnos(log2Up(wordSize - 1) downto 0).asUInt}
    is(M"011-1011") {dtosN := rtos}

    // Set all bits of top of stack to false by default
    default {dtosN := (default -> False)}

  }

  val funcTtoN  = (instr(6 downto 4).asUInt === 1) // Copy DTOS to DNOS
  val funcTtoR  = (instr(6 downto 4).asUInt === 2) // Copy DTOS to return stack
  val funcWrite = (instr(6 downto 4).asUInt === 3) 
  val funcIOW   = (instr(6 downto 4).asUInt === 4)
  val isALU     = (instr(instr.high downto (instr.high - 3) + 1) === B"b011");

  //assign mem_wr = !reboot & is_alu & func_write;
  //assign dout = st1;
  //assign io_wr = !reboot & is_alu & func_iow;

  //assign rstkD = (insn[13] == 1'b0) ? {{(`WIDTH - 14){1'b0}}, pc_plus_1, 1'b0} : st0;

  // Increment for data stack pointer
  val dStackPointerInc = SInt(log2Up(stackDepth) bits)

  // Handle update of data stack
  switch(instr(instr.high downto (instr.high - 3) + 1)) {

    // Literal (push value to data stack)
    is(M"1--") {dStackWrite := True; dStackPointerInc := 1}

    // Conditional jump (pop DTOS from data stack)
    is(M"001") {dStackWrite := False; dStackPointerInc := -1}

    // ALU instruction
    is(M"011"){dStackWrite := funcTtoN; dStackPointerInc := instr(1 downto 0).asSInt}

    // Don't change the data stack by default
    default {dStackWrite := False; dStackPointerInc := 0}

  }

  // Update the data stack pointer
  dStackPtr := (dStackPtr.asSInt + dStackPointerInc).asUInt

  // Increment for data stack pointer
  val rStackPointerInc = SInt(log2Up(stackDepth) bits)

  // Handle update of return stack
  switch(instr(instr.high downto (instr.high - 3) + 1)) {

    // Call instruction (push return address to stack)
    is(M"010") {rStackWrite := True; rStackPointerInc := 1}

    // Conditional jump (maybe we have to push)
    is(M"011") {rStackWrite := funcTtoR; rStackPointerInc := instr(3 downto 2).asSInt}

    // Don't change the return stack by default
    default {rStackWrite := False; rStackPointerInc := 0}

  }

  // Update the return stack pointer
  rStackPtr := (rStackPtr.asSInt + rStackPointerInc).asUInt

  // Handle the PC 
  switch(instr(instr.high downto (instr.high - 4) + 1)) {

    // Check for jump, call and cond. jump instruction
    is(M"000-",M"010-",M"001-") {pc := instr(addrWidth - 1 downto 0).asUInt}

    // Check for R -> PC field of an ALU instruction
    is(M"0111") {pc := rtos.asUInt}

    // By default goto next instruction
    default {pc := pc + 1}

  }

  // Use PC as address of instruction memory
  io.instrAddress := pc

}
