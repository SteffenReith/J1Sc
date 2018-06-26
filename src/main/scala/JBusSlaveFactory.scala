/*
 * Author: Steffen Reith (steffen.reith@hs-rm.de)
 *
 * Creation Date:  Sat Nov 25 11:43:36 GMT+1 2017
 * Module Name:    JBusSlaveFactory - Creates a slave factory of the JBus
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Remark: Initial code provided by Charles Papon (charles.papon.90@gmail.com)
 *
 */
import spinal.core._
import spinal.lib.bus.misc._

object JBusSlaveFactory {

  // Make a factory for the JBus
  def apply(bus : JBus) = new JBusSlaveFactory(bus)

}

class JBusSlaveFactory(bus : JBus) extends BusSlaveFactoryDelayed {

  // Get read/write address used on the bus
  def readAddress()  : UInt = bus.address
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

    // Init the read data wire to avoid a latch
    bus.readData := 0

    // Write permanently to the bus
    super.doNonStopWrite(bus.writeData)

    // Describe one data package transfered on the bus
    def doMappedElements(jobs : Seq[BusSlaveFactoryElement]) : Unit = super.doMappedElements (

      // Do the mapping of all bus signals
      jobs = jobs,
      askWrite = askWrite,
      askRead = askRead,
      doWrite = doWrite,
      doRead = doRead,
      writeData = bus.writeData,
      readData = bus.readData

    )

    // Check the current address on the bus (address decoder)
    switch(bus.address) {

      // For all jobs (e.g. registers) having a single address
      for ((address, jobs) <- elementsPerAddress if address.isInstanceOf[SingleMapping]) {

        is(address.asInstanceOf[SingleMapping].address) {

          // Wire the single address object to the bus
          doMappedElements(jobs)

        }

      }

    }

    // For all addresses that are not a single address (but a range) like RAMs etc.
    for ((address, jobs) <- elementsPerAddress if !address.isInstanceOf[SingleMapping]) {

      // Check if the current address on the bus is in the range
      when(address.hit(bus.address)) {

        // Handle the job by wiring the actual device to the bus
        doMappedElements(jobs)

      }

    }

  }

}
