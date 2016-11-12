/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Sat Nov 12 15:36:19 GMT+1 2016
 * Module Name:    SimpleBus - A simple bus used to communicate with GPIO peripherial components
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: <COMMITHASH>
 * Date: <AUTHORDATE>
 */
import spinal.core._
import spinal.lib._
import spinal.lib.bus.misc._

case class SimpleBus(addressWidth : Int, dataWidth : Int) extends Bundle with IMasterSlave {

  val enable      = Bool //Bus should be used when 'enable' is high
  val writeToBus  = Bool //High to write, low to read
  val address     = UInt(addressWidth bits) //Address (byte-aligned)
  val writeData   = Bits(dataWidth bits)
  val readData    = Bits(dataWidth bits)

  override def asMaster() : Unit = {
    out(enable, writeToBus, address, writeData)
    in(readData)
  }
}

case class SimpleBusSlaveFactory(bus : SimpleBus) extends BusSlaveFactoryDelayed {

  val readDataReg = Reg(Bits(bus.dataWidth bits)) //Give one cycle delay to reads (is that what you want ?)
  bus.readData := readDataReg

  override def build(): Unit = {

    for(element <- elements) element match {

      case element : BusSlaveFactoryNonStopWrite =>
        element.that.assignFromBits(bus.writeData(element.bitOffset, element.that.getBitsWidth bits))

      case _ =>

    }

    for((address, jobs) <- elementsPerAddress) {

      when(bus.address === address){

        when(bus.enable) {

          when(bus.writeToBus) {

            for (element <- jobs) element match {

              case element: BusSlaveFactoryWrite => {
                element.that.assignFromBits(bus.writeData(element.bitOffset, element.that.getBitsWidth bits))
              }

              case element: BusSlaveFactoryOnWrite => element.doThat()

              case _ =>

            }

          } otherwise {

            for (element <- jobs) element match {

              case element: BusSlaveFactoryRead => {
                readDataReg(element.bitOffset, element.that.getBitsWidth bits) := element.that.asBits
              }

              case element: BusSlaveFactoryOnRead => element.doThat()

              case _ =>

            }

          }

        }

      }

    }

  }

  override def busDataWidth: Int = bus.dataWidth

}
