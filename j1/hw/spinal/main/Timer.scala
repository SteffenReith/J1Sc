/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de) 
 *
 * Creation Date:  Sat Dec 3 17:33:06 GMT+1 2016
 * Module Name:    Timer16 - A programmable timer which generates interrupts for a 16 bit bus interface
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._
import spinal.lib.bus.misc.BusSlaveFactory

class Timer(cfg : TimerConfig) extends Component {

  // The width has to be even to be split into two halfs
  assert(cfg.width % 2 == 0, message = "ERROR: Timer width has to be even!")

  val io = new Bundle {

    val loadHigh = in Bool
    val loadLow  = in Bool
    val cmpHigh  = in UInt(cfg.width / 2 bits)
    val cmpLow   = in UInt(cfg.width / 2 bits)

    val enable            = in Bits(cfg.width / 2 bits)
    val accessEnableWrite = in Bool
    val enableState       = out Bits(cfg.width / 2  bits)

    val highState = out UInt(cfg.width / 2 bits)
    val lowState  = out UInt(cfg.width / 2 bits)

    val interrupt = out Bool

  }.setName("")

  // Register for holding the counter
  val cnt = Reg(UInt(cfg.width bits)) init(0)

  // Compare register (maximal values to count to)
  val cmp = Reg(UInt(cfg.width bits)) init(0)

  // Split the compare register into high and low part
  io.highState := cmp(cmp.high downto cfg.width / 2)
  io.lowState  := cmp(cfg.width / 2 - 1 downto 0)

  // Register the enable flag (adapt the width for the 1 bit register)
  val isEnabled = RegNextWhen(io.enable.orR, io.accessEnableWrite) init(False)
  io.enableState := (default -> isEnabled)

  // Check if the low word of the compare register has to be loaded
  when(io.loadLow) {

    // Load the compare value, reset the timer and disable it
    cmp(cfg.width / 2 - 1 downto 0) := io.cmpLow
    isEnabled := False
    cnt := 0

  }

  // Check if the high word of the compare register has to be loaded
  when(io.loadHigh) {

    // Load the compare value, reset the timer and disable it
    cmp(cmp.high downto cfg.width / 2) := io.cmpHigh
    isEnabled := False
    cnt := 0

  }

  // Maximal value of counter
  val maxCnt = cmp - 1

  // Check if the timer is enabled
  when(isEnabled && !(io.loadHigh || io.loadLow)) {

    // Count modulo
    when(cnt < maxCnt) {

      // Increment the timer
      cnt := cnt + 1

    }.otherwise {

      // Reset the timer
      cnt := 0

    }

  }

  // Generate the interrupt signal
  io.interrupt := isEnabled && (cnt === maxCnt) && !(io.loadHigh || io.loadLow)

  // Implement the bus interface
  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt) : Area = new Area {

    // The lower part of the compare register is mapped at address 0 and is of type r/w
    busCtrl.read(io.lowState, baseAddress + 0, bitOffset = 0)
    busCtrl.nonStopWrite(io.cmpLow, bitOffset = 0) // value will be constantly driven by the data of the memory bus

    // The higher part of the compare register is mapped at address 0 and is of type r/w
    busCtrl.read(io.highState, baseAddress + 1, bitOffset = 0)
    busCtrl.nonStopWrite(io.cmpHigh, bitOffset = 0) // value will be constantly driven by the data of the memory bus

    // Generate the write enable signals for loading the compare value
    io.loadLow  := busCtrl.isWriting(baseAddress + 0)
    io.loadHigh := busCtrl.isWriting(baseAddress + 1)

    // A r/w register for enabling the timer (anything != 0 means true)
    busCtrl.read(io.enableState, baseAddress + 2, bitOffset = 0)
    busCtrl.nonStopWrite(io.enable, bitOffset = 0) // the enable signal is constantly driven by the data of the memory bus

    // Generate a flag for write access of io.enable
    io.accessEnableWrite := busCtrl.isWriting(baseAddress + 2)

  }

}
