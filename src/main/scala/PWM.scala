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

  // Get the frequency of the current clock domain
  val currentFreq = ClockDomain.current.frequency.getValue

  // Calculate the divider value
  val dividerValue = (currentFreq / (pwmCfg.frequency * pwmCfg.numOfDutyCycles)).toInt

  // Write a message
  println("[J1Sc] PWM-frequency is " + pwmCfg.frequency.toBigDecimal + "Hz (Clock will be divided " + dividerValue + " times)")

  // Create the divider counter
  val divider = Reg(UInt(log2Up(dividerValue) bits)) init(0)

  // Decrement the divider
  divider := divider - 1

  // Generate a tick signal
  val dividerTick = (divider === 0)

  // Check if the divider has to be reloaded
  when(dividerTick) {

    // Reload the divider counter
    divider := dividerValue

  }

  // Create a free running duty cycle counter
  val cycleN = UInt(log2Up(pwmCfg.numOfDutyCycles) bits)
  val cycle = RegNextWhen(cycleN, dividerTick)
  cycleN := cycle + 1

  // Create all compare registers
  val compareRegs = Vec(for(i <- 0 to pwmCfg.numOfChannels - 1) yield {

    // Create the ith register
    RegNextWhen(io.newCompareValue.resize(log2Up(pwmCfg.numOfDutyCycles)).asUInt,
                io.writeEnable(i),
                U(log2Up(pwmCfg.numOfDutyCycles) bits, default -> false))

  })
  (io.compareRegs, compareRegs).zipped.foreach(_ := _.asBits)

  // Create the compare logic for all channels
  compareRegs.zipWithIndex.foreach{case (reg, i) => (io.pwmChannels(i) := (reg > cycle))}

  // Implement the bus interface
  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt) = new Area {

    // The value to be used as a new compare value is constantly driven by the bus
    busCtrl.nonStopWrite(io.newCompareValue, 0)

    // Make the compare register R/W
    for (i <- 0 to pwmCfg.numOfChannels - 1) {

      // A r/w register access for the ith interrupt vector
      busCtrl.read(io.compareRegs(i), baseAddress + i, 0)

      // Generate the write enable signal for the ith interrupt vector
      io.writeEnable(i) := busCtrl.isWriting(baseAddress + i)

    }

  }

}
