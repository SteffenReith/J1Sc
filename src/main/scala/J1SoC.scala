/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 * Committer: Steffen Reith
 *
 * Create Date:    Tue Sep 27 10:35:21 CEST 2016 
 * Module Name:    J1Core - Toplevel
 * Project Name:   J1Sc - A simple J1 implementation in scala
 *
 * Hash: 9b5a39bd614651c1d614feae12daf65e80ad5b3a
 * Date: Tue Sep 27 10:36:04 2016 +0200
 */
import spinal.core._
import spinal.lib._

class J1SoC extends Component {

  // Configure CPU core 
  val wordSize      =  16
  val stackDepth    = 256
  val addrWidth     =  13
  val startAddress  =   0

  // I/O ports
  val io = new Bundle {

    // I/O signals for memory data port
    val writeEnable = out Bool
    val dataAddress  = out UInt(addrWidth bits)
    val dataWrite = out Bits(wordSize bits)
    val dataRead  = in Bits(wordSize bits)

  }.setName("")

  // I/O signals for main memory
  val writeEnable = Bool
  val dataAddress = UInt(addrWidth bits)
  val dataWrite = Bits(wordSize bits)
  val dataRead = Bits(wordSize bits)

  // Wire the data bus to the outside world
  val io.writeEnable = writeEnable
  val io.dataAddress = dataAddress
  val io.dataWrite = dataWrite
  val io.dataRead = dataRead

  // Create main memory
  val content = List(B"1000 0000 0000 0111", // Push 7
                     B"0000 0000 0000 0001") // Jump 1
  val mainMem = Mem(Bits(wordSize bits),
                    1 << addrWidth) init (content)

  // Create data port for mainMem 
  mainMem.write(enable  = writeEnable,
                address = dataAddress,
                data    = dataWrite);
  dataRead := mainMem.readAsync(address = dataAddress)

  // Instruction port
  val instr = Bits(wordSize bits)
  val instrAddress = UInt(addrWidth bits)
  instr := mainMem.readAsync(address = instrAddress)

  // Create a new CPU
  val coreCPU = new J1Core

  // connect the CPU core
  coreCPU.io.writeEnable := writeEnable;
  coreCPU.io.dataAddress := dataAddress;
  coreCPU.io.dataWrite := dataWrite;
  coreCPU.io.dataRead := dataRead;
  coreCPU.io.instrAddress := instrAddress;
  coreCPU.io.instr := instr;

}

object J1SoC {

  // Make the reset synchron and use the rising edge
  val globalClockConfig = ClockDomainConfig (
    clockEdge        = RISING,
    resetKind        = SYNC,
    resetActiveLevel = HIGH)

  def main(args: Array[String]) {

    // Generate HDL files
    SpinalConfig(genVhdlPkg = false,
                 defaultConfigForClockDomains = globalClockConfig,
                 targetDirectory="gen/src/vhdl").generateVhdl(new J1SoC)
    SpinalConfig(genVhdlPkg = false,
                 defaultConfigForClockDomains = globalClockConfig,
                 targetDirectory="gen/src/verilog").generateVerilog(new J1SoC)

  }

}
