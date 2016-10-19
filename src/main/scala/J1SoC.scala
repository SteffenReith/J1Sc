/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 * Committer: Steffen Reith
 *
 * Create Date:    Tue Sep 27 10:35:21 CEST 2016 
 * Module Name:    J1Core - Toplevel
 * Project Name:   J1Sc - A simple J1 implementation in scala
 *
 * Hash: ece2c62952a1dd372f706f2c3f2c22e368410ec9
 * Date: Mon Oct 10 19:37:29 2016 +0200
 */
import spinal.core._
import spinal.lib._

class J1SoC (wordSize     : Int =  16,
             stackDepth   : Int = 256,
             addrWidth    : Int =  13,
             startAddress : Int =   0) extends Component {

  // Check the generic parameters
  assert(isPow2(stackDepth),"Depth of data stack has to be a power of two")

  // I/O ports
  val io = new Bundle {

    // I/O signals for memory data port
    val writeEnable = out Bool
    val dataAddress  = out UInt(addrWidth bits)
    val dataWrite = out Bits(wordSize bits)

  }.setName("")

  // Signals for main memory
  val writeEnable = Bool
  val dataAddress = UInt(addrWidth bits)
  val dataWrite = Bits(wordSize bits)
  val dataRead = Bits(wordSize bits)

  // Wire the data bus to the outside world
  io.writeEnable := writeEnable
  io.dataAddress := dataAddress
  io.dataWrite := dataWrite

  // Create main memory
  val content = List(B"1000_0000_0000_0111", // Push 7
                     B"1000_0000_0000_0011", // Push 3
                     B"0000_0000_0000_0100", // Jump 4
                     B"1000_0000_0000_0001", // Push 1
                     B"1000_0000_0000_1111", // Push 15
                     B"0110_0010_0000_0011", // Add and push
                     B"0110_0010_0000_0000", // Add and no push
                     B"0000_0000_0000_0111") // Jump 7

  val mainMem = Mem(Bits(wordSize bits),
                    content ++ List.fill((1 << addrWidth) - content.length)(B(0, wordSize bits)))

  // Create data port for mainMem 
  mainMem.write(enable  = writeEnable,
                address = dataAddress,
                data    = dataWrite);
  dataRead := mainMem.readSync(address = dataAddress)

  // Instruction port (read only)
  val instr = Bits(wordSize bits)
  val instrAddress = UInt(addrWidth bits)
  instr := mainMem.readSync(address = instrAddress)

  // Create a new CPU core
  val coreJ1CPU = new J1Core(wordSize, stackDepth, addrWidth, startAddress)

  // connect the CPU core
  writeEnable := coreJ1CPU.io.writeEnable
  dataAddress := coreJ1CPU.io.dataAddress
  dataWrite := coreJ1CPU.io.dataWrite
  coreJ1CPU.io.dataRead := dataRead
  instrAddress := coreJ1CPU.io.instrAddress
  coreJ1CPU.io.instr := instr

}

object J1SoC {

  // Make the reset synchron and use the rising edge
  val globalClockConfig = ClockDomainConfig(clockEdge        = RISING,
                                            resetKind        = SYNC,
                                            resetActiveLevel = HIGH)

  def main(args: Array[String]) {

    // Generate HDL files
    SpinalConfig(genVhdlPkg = true,
                 defaultConfigForClockDomains = globalClockConfig,
                 targetDirectory="gen/src/vhdl").generateVhdl({

                                                                // Set name for the synchronous reset
                                                                ClockDomain.current.reset.setName("clr")
                                                                new J1SoC(stackDepth = 8)

                                                              }).printPruned()
    SpinalConfig(defaultConfigForClockDomains = globalClockConfig,
                 targetDirectory="gen/src/verilog").generateVerilog({

                                                                      // Set name for the synchronous reset
                                                                      ClockDomain.current.reset.setName("clr")
                                                                      new J1SoC(stackDepth = 8)

                                                                    }).printPruned()

  }

}
