/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Tue Nov 1 00:19:43 GMT+1 2016
 * Module Name:    LEDBank - A simple LED bank
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: ec2c5c04988bb976040975bae0e46966d21cc9e9
 * Date: Tue Nov 1 15:34:51 2016 +0100
 */
import spinal.core._
import spinal.lib.bus.misc.BusSlaveFactory

class LEDBank(width     : Int     = 16,
              lowActive : Boolean = false) extends Component {

  val io = new Bundle {

    // I/O signals for memory data port
    val writeEnable = in Bool
    val ledState    = in Bits(width bits)
    val leds        = out Bits(width bits)

  }.setName("")

  // Register for holding the bit-vector storing the LED states
  val ledReg = Reg(Bits(width bits)) init(0)

  // Check for write mode
  when(io.writeEnable) {

    // Set register value
    ledReg := io.ledState

  }

  // Set output for the leds (invert it if asked for by the generic parameter)
  if (lowActive) io.leds := ~ledReg else io.leds := ledReg;

  // Implement the bus interface
  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt) = new Area {

    // The register is mapped at Address 0 and is of type r/w
    busCtrl.read(io.leds, baseAddress + 0)
    busCtrl.nonStopWrite(io.ledState, 0) // ledState will be constantly driven by the data of the memory bus
    io.writeEnable := busCtrl.isWriting(baseAddress + 0)

  }

}
