/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de) 
 *
 * Creation Date:  Wed Oct 11 16:00:44 GMT+2 2017
 * Module Name:    SSD - A simple seven-segment display component for the J1Sc project
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._
import spinal.lib._
import spinal.lib.bus.misc.BusSlaveFactory

case class Seg7() extends Bundle {

  // All segments of a seven-segment display
  val a, b, c, d, e, f, g = Bool()

}

class SSD(j1Cfg  : J1Config,
          ssdCfg : SSDConfig) extends Component {

  // Number of segments (without the dot) of a seven segment display
  val numOfSegs : Int = 7

  // Number of bits in a nibble
  val nibbleWidth : Int = 4

  // Check the generic parameters
  assert(Bool(ssdCfg.numOfDisplays <= j1Cfg.wordSize), "Error: Too many seven-segment displays!", FAILURE)

  val io = new Bundle {

    // Handle the data to be stored for the displays
    val newData       = in Bits (j1Cfg.wordSize bits)
    val enableNewData = in Bits (ssdCfg.numOfDisplays bits)
    val data          = out Vec(Bits(j1Cfg.wordSize bits), ssdCfg.numOfDisplays)

    // The physical signals for the segments and the dot
    val segments = out (Seg7())
    val dot      = out Bool

    // Selector for multiplexing
    val selector = out Bits(ssdCfg.numOfDisplays bits)

  }.setName("")

  // Convert a nibble to the segments
  def convToSegmentVector(nibble : Bits) : Bits = {

    // A look-up table to convert an UInt to the corresponding segments
    val convSegTab = Vec(B"0111111", B"0000110", B"1011011", B"1001111",
                         B"1100110", B"1101101", B"1111101", B"0000111",
                         B"1111111", B"1101111", B"1110111", B"1111100",
                         B"0111001", B"1011110", B"1111001", B"1110001")

    // Use the lookup-table to generate the segments
    convSegTab.read(nibble.asUInt)

  }

  // Write some infos about the configuration
  println("[J1Sc] SSD-frequency is " + ssdCfg.mplxFrequency.toBigDecimal + "Hz")
  println("[J1Sc]   Segments low-active: " + ssdCfg.invertSegments)
  println("[J1Sc]   Selector low-active: " + ssdCfg.invertSelector)

  // Create a slowed down clock domain for multiplexing the displays
  val ssdClockDomain = ClockDomain.current.newSlowedClockDomain(ssdCfg.mplxFrequency * ssdCfg.numOfDisplays)

  // Create an area for the slowed down selector used for multiplexing the displays
  val ssdArea = new ClockingArea(ssdClockDomain) {

    // Create a free running selector (one-hot)
    val selector = RegInit(B(ssdCfg.numOfDisplays bits, 0 -> true, default -> false))
    selector := selector(selector.high - 1 downto 0) ## selector.msb

  }

  // Create all registers holding the nibbles to be display (including the dot)
  val data = Vec(for(i <- 0 to ssdCfg.numOfDisplays - 1) yield {

    // Create the ith register and pack the provided data
    RegNextWhen(io.newData.msb ## io.newData(nibbleWidth - 1 downto 0),
                io.enableNewData(i),
                B((nibbleWidth + 1) bits, default -> false))

  })

  // Rearrange the packed register format for the databus
  data.zipWithIndex.foreach{case (reg, i) => (io.data(i) := reg.msb ##
                                                            reg(nibbleWidth - 1 downto 0).resize(j1Cfg.wordSize - 1))}

  // Copy the segments, dots and the selector to the output signals and make them low - active (if configured)
  val selData = data(OHToUInt(ssdArea.selector))
  val selSegs = convToSegmentVector(selData(selData.high - 1 downto 0))
  io.segments.assignFromBits(if (ssdCfg.invertSegments) ~selSegs else selSegs)
  io.dot := (if (ssdCfg.invertSegments) ~selData.msb else selData.msb)
  io.selector := (if (ssdCfg.invertSelector) ~ssdArea.selector else ssdArea.selector)

  // Implement the bus interface
  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt) = new Area {

    // The value to be used as a new compare value is constantly driven by the bus
    busCtrl.nonStopWrite(io.newData, 0)

    // Make the compare register R/W
    for (i <- 0 to ssdCfg.numOfDisplays - 1) {

      // A r/w register access for the ith interrupt vector
      busCtrl.read(io.data(i), baseAddress + i, 0)

      // Generate the write enable signal for the ith interrupt vector
      io.enableNewData(i) := busCtrl.isWriting(baseAddress + i)

    }

  }

}
