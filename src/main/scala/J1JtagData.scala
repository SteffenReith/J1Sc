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

case class J1JtagData(j1Cfg : J1Config) extends Bundle with IMasterSlave {

  // The data provided by the interface is valid
  val jtagDataValid = Bool()

  // Indicate that the CPU has to halted
  val jtagStall = Bool()

  // Indicate that the CPU memory is managed by the jtag interface
  val jtagCaptureMemory = Bool()

  // Hold address and data for the CPU memory
  val jtagCPUAdr  = Bits(j1Cfg.adrWidth bits)
  val jtagCPUWord = Bits(j1Cfg.wordSize bits)

  def init(adr : Int = 0, data : Int = 0) : J1JtagData  = {

    // Make a clone
    val ret = cloneOf(this)

    // Init the bundle (deactivate stall, reset and capture memory)
    ret.jtagDataValid := False
    ret.jtagStall := False
    ret.jtagCaptureMemory := False
    ret.jtagCPUAdr := B(adr, j1Cfg.adrWidth bits)
    ret.jtagCPUWord := B(data, j1Cfg.wordSize bits)

    // Return the clone
    ret

  }

  // Clear the instance
  def clear() : Unit = {

    // Init all members
    this.jtagDataValid := False
    this.jtagStall := False
    this.jtagCaptureMemory := False
    this.jtagCPUAdr := B(0, j1Cfg.adrWidth bits)
    this.jtagCPUWord := B(0, j1Cfg.wordSize bits)

  }

  // Set the data directions when used as a master
  override def asMaster() : Unit = {

    // Write data to the bus
    out(jtagStall, jtagCaptureMemory, jtagCPUAdr, jtagCPUWord)

  }

}
