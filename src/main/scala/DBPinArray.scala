/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Creation Date:  Tue Oct 31 11:11:13 GMT+1 201  Tue Oct 31 11:11:13 GMT+1 20177 
 * Module Name:    DBPinArray - Provides an array of debounced input pins
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */

import spinal.core._
import spinal.lib._
import spinal.lib.bus.misc.BusSlaveFactory

class DBPinArray(j1Cfg         : J1Config,
                 dbPinArrayCfg : DBPinArrayConfig) extends Component {

  // Check the generic parameters
  assert(Bool(dbPinArrayCfg.numOfPins <= j1Cfg.wordSize), "Error: Too many pins in one array!", FAILURE)

  // Signals used for the internal bus
  val bus = new Bundle {

    // Debounced pins
    val dbPins = out Bits (j1Cfg.wordSize bits)

  }

  // Signals for a physical connection
  val io = new Bundle {

    // Physical input pins
    val inputPins = in Bits(dbPinArrayCfg.numOfPins bits)

  }

  // Create the register holding the debounced inputs
  val debounced = Reg(Bits(dbPinArrayCfg.numOfPins bits))

  // Create an alarm timer for the timeout
  val timeOut = Timeout(dbPinArrayCfg.waitTime)

  // Clear the timeout if nothing has chanced
  when (debounced === io.inputPins ) {timeOut.clear()}

  // When the alarm is active the new input is taken
  when (timeOut) {debounced := io.inputPins}

  // Clone the register holding the debounced input signals
  bus.dbPins := debounced.resize(j1Cfg.wordSize)

  // Implement the bus interface
  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt) = new Area {

    // A r/w register access for the ith interrupt vector
    busCtrl.read(bus.dbPins, baseAddress, 0)


  }

}
