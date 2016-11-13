/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Tue Nov 1 14:34:09 GMT+1 2016
 * Module Name:    J1SoC - A small but complete system based on the J1-core
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: ec2c5c04988bb976040975bae0e46966d21cc9e9
 * Date: Tue Nov 1 15:34:51 2016 +0100
 */
import spinal.core._

class J1SoC extends Component {

  val io = new Bundle {

    val leds = out Bits(ledBankWidth bits)

  }

  // Parameters to configure the CPU
  def wordSize     = 16
  def addrWidth    = 13
  def ledBankWidth = 16

  // Create a new CPU core
  val cpuCore = new J1(wordSize = wordSize,
                       dataStackIdxWidth = 4,
                       returnStackIdxWidth = 3,
                       addrWidth = addrWidth,
                       startAddress = 0)

  // Create a bus for the LED bank
  val ledBus = SimpleBus(addrWidth, wordSize)
  val ledBusCtrl = SimpleBusSlaveFactory(ledBus)

  // Connect the bus and enable it permanently
  ledBus.enable    := True
  ledBus.writeMode := cpuCore.io.writeEnable
  ledBus.address   := cpuCore.io.dataAddress
  ledBus.readData  := cpuCore.io.dataRead
  ledBus.writeData := cpuCore.io.dataWrite

  // Create a LED bank at address 0x00 and connect it to the outside world
  val leds = new LEDBank(ledBankWidth, False)
  val ledsBridge = leds.driveFrom(ledBusCtrl, 0x00)
  io.leds := leds.io.leds

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
