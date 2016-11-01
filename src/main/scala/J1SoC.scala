/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Tue Nov 1 14:34:09 GMT+1 2016
 * Module Name:    J1SoC - A small but complete system based on the J1-core
 * Project Name:   J1Sc - A simple J1 implementation in Scala
 *
 * Hash: <COMMITHASH>
 * Date: <AUTHORDATE>
 */
import spinal.core._
import spinal.lib._

class J1SoC extends Component {

  // Create a new CPU core
  val core = new J1(wordSize = 16,
                    dataStackIdxWidth = 4,
                    returnStackIdxWidth = 3,
                    addrWidth = 13,
                    startAddress = 0)

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
      new J1SoC()

    }).printPruned()
    SpinalConfig(defaultConfigForClockDomains = globalClockConfig,
      targetDirectory="gen/src/verilog").generateVerilog({

      // Set name for the synchronous reset
      ClockDomain.current.reset.setName("clr")
      new J1SoC()

    }).printPruned()

  }

}
