/*
 * Author: Steffen Reith (steffen.reith@hs-rm.de)
 *
 * Creation Date:  Sat Nov 12 15:36:19 GMT+1 2016
 * Module Name:    JBus - A simple bus used for the components of the J1 ecosystem
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Remark: Initial code provided by Charles Papon (charles.papon.90@gmail.com)
 *
 */
import spinal.core._
import spinal.lib._

case class J1Bus(cfg : J1Config) extends Bundle with IMasterSlave {

  // Width of addresses and data
  def adrWidth  : Int = cfg.wordSize
  def dataWidth : Int = cfg.wordSize

  // Data and control signals used by 'SimpleBus'
  val enable    = Bool // Bus can be used when 'enable' is high
  val writeMode = Bool // High to write data, low to read data
  val address   = UInt(adrWidth bits) // Address (byte-aligned)
  val writeData = Bits(dataWidth bits)
  val readData  = Bits(dataWidth bits)

  // Created a copy of the current bus which signals are delayed by 'delayCnt' ticks
  def delayIt(ticks : Int = 1) : J1Bus = {

    // Check for proper parameter
    require (ticks >= 0, "Error: ticks has to be at least 0")

    // Make a copy
    val retVal = cloneOf(this)

    // Don't delay the data to be read hence we have one wait state for a read operation
    this.readData := retVal.readData

    // Delay all other signals and wire them
    retVal.address   := Delay(this.address,    ticks)
    retVal.enable    := Delay(this.enable,     ticks)
    retVal.writeMode := Delay(this.writeMode,  ticks)
    retVal.writeData := Delay(this.writeData,  ticks)

    // Return the delayed version of the actual SimpleBus object
    retVal

  }

  // Methods to connect SimpleBus objects
  def << (that : J1Bus) : Unit = {

    // Simply wire the signals of 'this' and 'that'
    that.enable    := this.enable
    that.writeMode := this.writeMode
    that.address   := this.address
    that.writeData := this.writeData
    this.readData  := that.readData

  }
  def >>(that : J1Bus) : Unit = that << this

  // Set the data directions when used as a master
  override def asMaster() : Unit = {

    // Write data to the bus
    out(enable, writeMode, address, writeData)

    // Read data from the bus
    in(readData)

  }

}
