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

  // Top of stack (do not init, hence undefined value after startup)
  val tos = Reg(Bits(wordSize bits))

  // Programm counter (PC)
  val pc = Reg(UInt(addrWidth bits)) init(startAddress);

  // Instruction to be excuted
  val instr = io.instr

  // ALU block
  switch(instr(instr.high downto (instr.high - 8) + 1)) {

    // Literal
    is(M"1-------") {tos := instr(instr.high - 1 downto 0).resize(wordSize)}

    default {tos := (default -> false)}

  }

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
