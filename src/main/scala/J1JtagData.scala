/*
 * Author: Steffen Reith (steffen.reith@hs-rm.de)
 *
 * Creation Date:  Fri Jan  3 11:55:50 CET 2020
 * Module Name:    J1JtagData - Contains a bundle of data provided by the jtag interface
 * Project Name:   J1Sc       - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._
import spinal.lib.IMasterSlave

case class J1JtagBus(j1Cfg : J1Config) extends Bundle with IMasterSlave {

  // Indicate that the CPU has to halted
  val jtagStall = Bool

  // Indicate that we should reset the CPU
  val jtagReset = Bool

  // Indicate that the CPU memory is managed by the jtag interface
  val jtagCaptureMemory = Bool

  // Hold address and data for the CPU memory
  val jtagCPUAdr  = Bits(j1Cfg.adrWidth bits)
  val jtagCPUWord = Bits(j1Cfg.wordSize bits)

  // Set the data directions when used as a master
  override def asMaster() : Unit = {

    // Write data to the bus
    out(jtagStall, jtagReset, jtagCaptureMemory, jtagCPUAdr, jtagCPUWord)

  }

}
