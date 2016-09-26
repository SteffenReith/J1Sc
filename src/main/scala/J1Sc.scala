/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Create Date:    Tue Sep 20 15:07:10 CEST 2016 
 * Module Name:    J1Sc - Toplevel
 * Project Name:   J1Sc - A simple J1 implementation
 *
 * Hash: bc807384d0d2b794942f61aab9c8f504ae031538
 * Date: Wed Sep 21 00:06:08 2016 +0200
 */
import spinal.core._
import spinal.lib._

class J1Sc (wordSize     : Int = 32,
            stackDepth   : Int = 256,
            addrWidth    : Int = 12,
            startAddress : Int = 0 ) extends Component {

  // I/O ports
  val io = new Bundle {

    // Address of next instruction
    //val instrAdr = out (UInt(addrWidth bits))

    // Next instruction
    val instr = in (Bits(wordSize bits))

  }.setName("")

  // Data stack pointer (init to first entry)
  val dStackPtr = Reg(UInt(log2Up(stackDepth) bits)) init(0)  

  // Return stack pointer (init to first entry)
  val rStackPtr = Reg(UInt(log2Up(stackDepth) bits)) init(0)

  // Data and return stack
  val dStack = Mem(Bits(wordSize bits), wordCount = stackDepth)
  val rStack = Mem(Bits(wordSize bits), wordCount = stackDepth)

  // Top of stack (do not init, hence undefined value after startup)
  val tos = Reg(Bits(wordSize bits))

  // Next of data stack
  val nos = dStack.readAsync(address = dStackPtr);

  // Programm counter (PC)
  val pc = Reg(UInt(addrWidth bits)) init(startAddress);

  // Instruction to be excuted
  val instr = io.instr

  // Instruction decoder for data stack related operations
  switch(instr(instr.high downto (instr.high - 8) + 1)) {

    // Literal
    is(M"1-------") {tos := instr(instr.high - 1 downto 0).resize(wordSize)}

    // Jump instruction (do not change TOS)
    is(M"000-----") {tos := tos}

    // Call instruction (do not change TOS)
    is(M"000-----") {tos := tos}

    // Conditional jump (pop the 0 at TOS)
    is(M"001-----") {tos := nos}

    // ALU operations using tos and nos
    is(M"011-0000") {tos := tos}
    is(M"011-0001") {tos := nos}

    // Arithmetic and logical operations (ALU)
    is(M"011-0010") {tos := (tos.asUInt + nos.asUInt).asBits}
    is(M"011-0011") {tos := tos & nos}
    is(M"011-0100") {tos := tos | nos}
    is(M"011-0101") {tos := tos ^ nos}
    is(M"011-0110") {tos := ~tos}
    is(M"011-1001") {tos := tos >> nos(log2Up(wordSize - 1) downto 0).asUInt}
    is(M"011-1010") {tos := tos << nos(log2Up(wordSize - 1) downto 0).asUInt}

    // Set all bits of top of stack to false by default
    default {tos := (default -> false)}

  }

  // Generate internal control signals
  val funcTtoN  = (instr(6 downto 4).asUInt === 1)
  val funcTtoR  = (instr(6 downto 4).asUInt === 2)
  val funcWrite = (instr(6 downto 4).asUInt === 3)
  val funcIOW   = (instr(6 downto 4).asUInt === 4)
  val isALU     = (instr(instr.high downto (instr.high - 3) + 1) === B"b011");

  assign mem_wr = !reboot & is_alu & func_write;
  assign dout = st1;
  assign io_wr = !reboot & is_alu & func_iow;

  assign rstkD = (insn[13] == 1'b0) ? {{(`WIDTH - 14){1'b0}}, pc_plus_1, 1'b0} : st0;

}

object J1Sc {

  // Make the reset synchron
  val globalClockConfig = ClockDomainConfig (
    clockEdge        = RISING,
    resetKind        = SYNC,
    resetActiveLevel = HIGH
  )

  def main(args: Array[String]) {

    // Generate HDL files
    SpinalConfig(genVhdlPkg = false,
                 defaultConfigForClockDomains = globalClockConfig,
                 targetDirectory="gen/src/vhdl").generateVhdl(new J1Sc)
    SpinalConfig(genVhdlPkg = false,
                 defaultConfigForClockDomains = globalClockConfig,
                 targetDirectory="gen/src/verilog").generateVerilog(new J1Sc)

  }

}
