/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Sun Mar 12 00:53:59 GMT+1 2017
 * Module Name:    PMod - An interface to a Digilent PMOD interface
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: <COMMITHASH>
 * Date: <AUTHORDATE>
 */
import spinal.core._
import spinal.lib.bus.misc.BusSlaveFactory

class GPIO(j1Cfg : J1Config, gpioCfg : GPIOConfig) extends Component {

  // Give a warning if the GPIO-register is not used as a PMOD
  assert(Bool(gpioCfg.width != 8), "Warning: A PMod according to the digilent specification has width 8!", ERROR)

  val io = new Bundle {

    // Ports used for the direction register
    val dirEnable  = in Bool
    val dirState   = in Bits(gpioCfg.width bits)
    val directions = out Bits(gpioCfg.width bits)

    // Ports used the the data register
    val dataEnable = in Bool
    val dataState  = in Bits(gpioCfg.width bits)
    val dataIn     = in Bits(gpioCfg.width bits)
    val dataOut    = out Bits(gpioCfg.width bits)


  }.setName("")

  // Register for holding the direction of the GPIO register
  val dirReg = RegNextWhen(io.dirState, io.dirEnable) init(0)

  // Propagate the contents of the direction register to the interface
  io.directions := dirReg

  // Register for holding the IO data
  val dataReg = RegNext(dataRegN) init(0)

  // Check if the register was addressed by a bus transfer
  val dataRegN = Bits(gpioCfg.width bits)
  when (io.dataEnable) {

    // Select and update the bits to be read / written
    dataRegN := (io.dataIn & (~io.directions)) | (io.dataState & io.directions)

  } .otherwise {

    // Update only bits to be read
    dataRegN := dataReg | (io.dataIn & (~io.directions))

  }

  // Propagate the contents of the data register to the interface
  io.dataOut := dataReg

  // Implement the bus interface
  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt) = new Area {

    // The direction register is mapped at address 0 and is of type r/w
    busCtrl.read(io.directions, baseAddress + 0, 0)
    busCtrl.nonStopWrite(io.dirState, 0) // contents of direction register will be constantly driven by the bus
    io.dirEnable := busCtrl.isWriting(baseAddress + 0)

    // The data register is mapped at address 4 and is of type r/w
    busCtrl.read(io.dataOut, baseAddress + 4, 0)
    busCtrl.nonStopWrite(io.dataState, 0) // contents of direction register will be constantly driven by the bus
    io.dataEnable := busCtrl.isWriting(baseAddress + 4)

  }

}
