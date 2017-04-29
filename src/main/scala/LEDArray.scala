/*
 * Author: Steffen Reith (steffen.reith@hs-rm.de)
 *
 * Creation Date:  Tue Nov 1 00:19:43 GMT+1 2016
 * Module Name:    LEDBank - A simple LED bank
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._
import spinal.lib.bus.misc.BusSlaveFactory

class LEDArray(cfg : LEDArrayConfig) extends Component {

  val io = new Bundle {

    // I/O signals for memory data port
    val writeEnable = in Bool
    val ledValue    = in Bits (cfg.width bits)
    val leds        = out Bits(cfg.width bits)

  }.setName("")

  // Register for holding the bit-vector storing the LED states
  val ledReg = Reg(Bits(cfg.width bits)) init (0)

  // Check for write mode
  when(io.writeEnable) {

    // Set register value
    ledReg := io.ledValue

  }

  // Set output for the leds (invert it if asked for by the generic parameter)
  if (cfg.lowActive) io.leds := ~ledReg else io.leds := ledReg;

  // Implement the bus interface
  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt) = new Area {

    // The register is mapped at address 0 and is of type r/w
    if (cfg.lowActive) {

      // Negate to get the register content
      busCtrl.read(~io.leds, baseAddress + 0, 0)

    } else {

      // Simply give back the register content
      busCtrl.read(io.leds, baseAddress + 0, 0)

    }
    busCtrl.nonStopWrite(io.ledValue, 0) // ledState will be constantly driven by the data of the memory bus
    io.writeEnable := busCtrl.isWriting(baseAddress + 0)

  }

}
