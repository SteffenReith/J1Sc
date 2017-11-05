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

class LEDArray(j1Cfg  : J1Config,
               ledCfg : LEDArrayConfig) extends Component {

  // Check the generic parameters
  assert(Bool(ledCfg.width <= j1Cfg.wordSize), "Error: Too many pwm channels!", FAILURE)

  val bus = new Bundle {

    // Bus signals for the internal register
    val writeEnable = in Bool
    val ledValue    = in Bits (ledCfg.width bits)

  }.setName("")

  val io = new Bundle {

    // The physical led registers
    val leds        = out Bits(ledCfg.width bits)

  }.setName("")

  // Register for holding the bit-vector storing the LED states
  val ledReg = Reg(Bits(ledCfg.width bits)) init (0)

  // Check for write mode
  when(bus.writeEnable) {

    // Set register value
    ledReg := bus.ledValue

  }

  // Set output for the leds (invert it if asked for by the generic parameter)
  if (ledCfg.lowActive) io.leds := ~ledReg else io.leds := ledReg;

  // Implement the bus interface
  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt) = new Area {

    // The register is mapped at address 0 and is of type r/w
    if (ledCfg.lowActive) {

      // Negate to get the register content
      busCtrl.read(~io.leds, baseAddress + 0, 0)

    } else {

      // Simply give back the register content
      busCtrl.read(io.leds, baseAddress + 0, 0)

    }
    busCtrl.nonStopWrite(bus.ledValue, 0) // ledState will be constantly driven by the data of the memory bus
    bus.writeEnable := busCtrl.isWriting(baseAddress + 0)

  }

}
