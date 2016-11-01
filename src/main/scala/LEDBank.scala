/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Tue Nov 1 00:19:43 GMT+1 2016
 * Module Name:    LEDBank - A simple LED bank
 * Project Name:   J1Sc - A simple J1 implementation in Scala
 *
 * Hash: <COMMITHASH>
 * Date: <AUTHORDATE>
 */
import spinal.core._
import spinal.lib.bus.misc.BusSlaveFactory

class LEDBank(width     : Int = 16,
              lowActive : Bool = False) extends Component {

  val io = new Bundle {

    // I/O signals for memory data port
    val writeEnable = in Bool
    val ledState = in Bits(width bits)
    val leds = out Bits(width bits)

  }

  // Register for holding the bit-vector storing the LED states
  val ledReg = Reg(Bits(width bits)) init(0)

  // Check for write mode
  when(io.writeEnable) {

    // Set register value
    ledReg := io.ledState

  }

  // Set output for the leds (invert it if asked for by the generic parameter)
  io.leds := lowActive ? ~ledReg | ledReg

  // Implement the bus interface
  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt)(ledState : Seq[Bool]) = new Area {

    // The register is mapped at Address 0 and is of type r/w
    io.ledState := busCtrl.createReadWrite(Bits(ledState.length bits), baseAddress + 0, 0) init(0)
    io.writeEnable setWhen(busCtrl.isWriting(baseAddress + 0))

  }

}
