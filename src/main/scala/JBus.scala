/*
 * Author: Steffen Reith (steffen.reith@hs-rm.de)
 *
 * Creation Date:  Sat Nov 12 15:36:19 GMT+1 2016
 * Module Name:    SimpleBus - A simple bus which consists out of address / dataIn / dataOut / writeMode / enable
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Remark: Initial code provided by Charles Papon (charles.papon.90@gmail.com)
 *
 */
import spinal.core._
import spinal.lib._
import spinal.lib.bus.misc._

case class J1Bus(cfg : J1Config) extends Bundle with IMasterSlave {

  // Width of addresses and data
  def adrWidth  = cfg.wordSize
  def dataWidth = cfg.wordSize

  // Data and control signals used by 'SimpleBus'
  val enable    = Bool // Bus can be used when 'enable' is high
  val writeMode = Bool // High to write data, low to read data
  val address   = UInt(adrWidth bits) // Address (byte-aligned)
  val writeData = Bits(dataWidth bits)
  val readData  = Bits(dataWidth bits)

  // Created a copy of the current bus which signals are delayed by 'delayCnt' ticks
  def delayIt(ticks : Int = 1) : J1Bus = {

    // Check for proper parameter
    require (ticks >= 0, "Error: delayCnt has to be at least 0")

    // Make a copy
    val retVal = cloneOf(this)

    // Don't delay the data to be read hence we have one wait state for read operation
    this.readData := retVal.readData

    // Delay all other signals and wire them
    retVal.address   := Delay(this.address,    ticks)
    retVal.enable    := Delay(this.enable,     ticks)
    retVal.writeMode := Delay(this.writeMode,  ticks)
    retVal.writeData := Delay(this.writeData,  ticks)

    // Return the delayed version of the actual SimpleBus object
    return retVal

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

  // This is called by 'apply' when the master-object is called with data (-> side effect write/read data)
  override def asMaster() : Unit = {

    // Write data to the bus
    out(enable, writeMode, address, writeData)

    // Read data from the bus
    in(readData)

  }

}

class J1BusSlaveFactory(bus : J1Bus) extends BusSlaveFactoryDelayed {

  // Get read/write address used on the bus
  def readAdress()   : UInt = bus.address
  def writeAddress() : UInt = bus.address

  // Peripherals cannot stop bus cycles
  def readHalt()  : Unit = throw new Exception("Unsupported feature")
  def writeHalt() : Unit = throw new Exception("Unsupported feature")

  // Ask for read/write access
  val askWrite = bus.enable &&  bus.writeMode
  val askRead  = bus.enable && !bus.writeMode

  // Simple bus has no halting signal, hence do read/write means ask for read/write
  def doWrite = askWrite
  def doRead  = askRead

  // Tell the width of the data bus
  override def busDataWidth : Int = bus.dataWidth

  // Build the bridging logic between master and slave
  override def build() : Unit = {

    super.doNonStopWrite(bus.writeData)

    def doMappedElements(jobs : Seq[BusSlaveFactoryElement]) = super.doMappedElements(

      jobs = jobs,
      askWrite = askWrite,
      askRead = askRead,
      doWrite = doWrite,
      doRead = doRead,
      writeData = bus.writeData,
      readData = bus.readData

    )

    switch(bus.address) {

      for ((address, jobs) <- elementsPerAddress if address.isInstanceOf[SingleMapping]) {

        is(address.asInstanceOf[SingleMapping].address) {

          doMappedElements(jobs)

        }

      }

    }

    for ((address, address) <- elementsPerAddress if !address.isInstanceOf[SingleMapping]) {

      when(address.hit(bus.PADDR)){

        doMappedElements(jobs)

      }

    }

  }

}
