/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de) 
 *
 * Creation Date:  Thu Sep 28 10:52:01 GMT+2 2017
 * Module Name:    PWM - A simple PWM component for the J1Sc project
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._
import spinal.lib.bus.misc.BusSlaveFactory

class PWM(j1Cfg  : J1Config,
          pwmCfg : PWMConfig) extends Component {

  // Check the generic parameters
  assert(Bool(isPow2(pwmCfg.numOfDutyCycles)), "Error: The number of duty cycles has to be a power of 2!", FAILURE)

  val io = new Bundle {

    val newCompareValue = in Bits(j1Cfg.wordSize bits)

    val writeEnable = in Bits(pwmCfg.numOfChannels bits)
    val compareRegs = out Vec(Bits(log2Up(pwmCfg.numOfDutyCycles) bits), pwmCfg.numOfChannels)

    val pwmChannels = out Bits(pwmCfg.numOfChannels bits)

  }.setName("")

  // Write a message
  println("[J1Sc] PWM-frequency is " + pwmCfg.pwmFrequency.toBigDecimal + "Hz")

  // Create a slowed down clock domain for the PWM
  val pwmClockDomain = ClockDomain.current.newSlowedClockDomain(pwmCfg.pwmFrequency * pwmCfg.numOfDutyCycles)

  // Create an area for the slowed down duty cycle counter
  val pwmArea = new ClockingArea(pwmClockDomain) {

    // Create a free running duty cycle counter
    val cycle = Reg(UInt(log2Up(pwmCfg.numOfDutyCycles) bits))
    cycle := cycle + 1

  }

  // Create all compare registers
  val compareRegs = Vec(for(i <- 0 to pwmCfg.numOfChannels - 1) yield {

    // Create the ith register
    RegNextWhen(io.newCompareValue.resize(log2Up(pwmCfg.numOfDutyCycles)).asUInt,
                io.writeEnable(i),
                U(log2Up(pwmCfg.numOfDutyCycles) bits, default -> false))

  })
  (io.compareRegs, compareRegs).zipped.foreach(_ := _.asBits)

  // Create the compare logic for all channels
  compareRegs.zipWithIndex.foreach{case (reg, i) => (io.pwmChannels(i) := (reg > pwmArea.cycle))}

  // Implement the bus interface
  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt) = new Area {

    // The value to be used as a new compare value is constantly driven by the bus
    busCtrl.nonStopWrite(io.newCompareValue, 0)

    // Make the compare register R/W
    for (i <- 0 to pwmCfg.numOfChannels - 1) {

      // A r/w register access for the ith compare register
      busCtrl.read(io.compareRegs(i), baseAddress + i, 0)

      // Generate the write enable signal for the ith compare register
      io.writeEnable(i) := busCtrl.isWriting(baseAddress + i)

    }

  }

}
