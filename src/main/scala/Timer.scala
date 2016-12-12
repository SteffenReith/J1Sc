/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Sat Dec 3 17:33:06 GMT+1 2016
 * Module Name:    Timer16 - A programmable timer which generates interrupts for a 16 bit bus interface
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: ddd388a1d861c04cade760aad77d80d1ea8eca8d
 * Date: Mon Dec 12 03:50:41 2016 +0100
 */
import spinal.core._
import spinal.lib.bus.misc.BusSlaveFactory

class Timer(cfg : TimerConfig) extends Component {

  // The width has to be even to be split into two halfs
  assert(Bool((cfg.width) % 2 == 0), "Error: Timer width has to be even!", FAILURE)

  val io = new Bundle {

    val loadHigh = in Bool
    val loadLow  = in Bool
    val cmpHigh  = in UInt(cfg.width / 2 bits)
    val cmpLow   = in UInt(cfg.width / 2 bits)

    val enable = in Bits(cfg.width / 2 bits)

    val highState = out UInt(cfg.width / 2 bits)
    val lowState  = out UInt(cfg.width / 2 bits)

    val enableState = out Bits(cfg.width / 2  bits)

    val interrupt = out Bool

  }.setName("")

  // Register for holding the counter
  val cnt = Reg(UInt(cfg.width bits)) init (0)

  // Compare register (maximal values to count to)
  val cmp = Reg(UInt(cfg.width bits)) init (0)
  io.highState := cmp(cmp.high downto cfg.width / 2)
  io.lowState := cmp(cfg.width / 2 - 1 downto 0)

  // Register the enable flag (adapt the width for the 1 bit register)
  val isEnabled = Reg(Bool) init (False)
  isEnabled := io.enable.orR
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

  // Check if the timer is enabled
  when(isEnabled && !(io.loadHigh || io.loadLow)) {

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
  io.interrupt := isEnabled && (cnt === 0) && !(io.loadHigh || io.loadLow)

  // Implement the bus interface
  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt) = new Area {

    // The lower part of the compare register is mapped at address 0 and is of type r/w
    busCtrl.read(io.lowState, baseAddress + 0, 0)
    busCtrl.nonStopWrite(io.cmpLow, 0) // value will be constantly driven by the data of the memory bus

    // The higher part of the compare register is mapped at address 0 and is of type r/w
    busCtrl.read(io.highState, baseAddress + 1, 0)
    busCtrl.nonStopWrite(io.cmpHigh, 0) // value will be constantly driven by the data of the memory bus

    // Generate the write enable signals for loading the compare value
    io.loadLow  := busCtrl.isWriting(baseAddress + 0)
    io.loadHigh := busCtrl.isWriting(baseAddress + 1)

    // A r/w register for enabling the timer (anything != 0 means true)
    busCtrl.read(io.enableState, baseAddress + 2, 0)
    busCtrl.nonStopWrite(io.enable, 0) // the enable signal is constantly driven by the data of the memory bus

  }

}
