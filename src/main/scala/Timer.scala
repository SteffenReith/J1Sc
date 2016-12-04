/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Sat Dec 3 17:33:06 GMT+1 2016
 * Module Name:    Timer16 - A programmable timer which generates interrupts for a 16 bit bus interface
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: <COMMITHASH>
 * Date: <AUTHORDATE>
 */
import spinal.core._
import spinal.lib._
import spinal.lib.bus.misc.BusSlaveFactory

class Timer(cfg : TimerConfig) extends Component {

  // The width has to be even to be split into two halfs
  assert(Bool((cfg.width) % 2 == 0), "Error: Timer width has to be equal!", FAILURE)

  val io = new Bundle {

    val loadHigh  = in Bool
    val loadLow   = in Bool
    val valueHigh = in UInt (cfg.width / 2 bits)
    val valueLow  = in UInt (cfg.width / 2 bits)
    val enable   = in Bool

    val interrupt = out Bool

  }.setName("")

  // Register for holding the counter
  val cnt = Reg(UInt(cfg.width bits)) init (0)

  // Compare register (maximal values to count to)
  val cmp = Reg(UInt(cfg.width bits)) init (0)

  // Holds the enable flag
  val enabled = Reg(Bool) init(False)

  // Register the enable signal
  when(io.enable) {enabled := True}

  // Check if the high word of the compare register has to be loaded
  when(io.loadHigh) {

    // Load the compare value, reset the timer and disable it
    cmp(cmp.high downto cfg.width / 2) := io.valueHigh
    enabled := False
    cnt := 0

  }

  // Check if the low word of the compare register has to be loaded
  when(io.loadHigh) {

    // Load the compare value, reset the timer and disable it
    cmp(cfg.width / 2 - 1 downto 0) := io.valueLow
    enabled := False
    cnt := 0

  }

  // Check if the timer is enabled
  when(enabled && !(io.loadHigh || io.loadLow)) {

    // Count modulo
    when(cnt < cmp - 1) {

      // Increment the timer
      cnt := cnt + 1

    }.otherwise {

      // Reset the timer
      cnt := 0

    }

  }

  // Generate the interrupt signal
  io.interrupt := enabled & (cnt === 0) & !(io.loadHigh || io.loadLow)

  // Implement the bus interface
  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt) = new Area {

    // The lower part of the compare register is mapped at address 0 and is of type r/w
    busCtrl.read(io.valueLow, baseAddress + 0, 0)
    busCtrl.nonStopWrite(io.valueLow, 0) // value will be constantly driven by the data of the memory bus

    // The higher part of the compare register is mapped at address 0 and is of type r/w
    busCtrl.read(io.valueHigh, baseAddress + 1, 0)
    busCtrl.nonStopWrite(io.valueHigh, 0) // value will be constantly driven by the data of the memory bus

    // Generate the write enable signals for loading the compare value
    io.loadLow  := busCtrl.isWriting(baseAddress + 0)
    io.loadHigh := busCtrl.isWriting(baseAddress + 1)

    // Enable the time by a simple write access
    io.enable := busCtrl.isWriting(baseAddress + 3)

  }

}
