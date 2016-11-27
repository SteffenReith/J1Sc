/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Fri Nov 25 13:04:23 GMT+1 2016
 * Module Name:    InterruptCtrl - A small interrupt controller for the J1 CPU
* Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: <COMMITHASH>
 * Date: <AUTHORDATE>
 */
import spinal.core._
import spinal.lib._

class InterruptCtrl(noOfInterrupts : Int) extends Component {

  // Check the number of interrupts
  assert(isPow2(noOfInterrupts), "Warning: Specify a power of 2 as number of interrupts")

  var io = new Bundle {

    val intsE = in Bits(noOfInterrupts bits)

    val intNo = out UInt(log2Up(noOfInterrupts) bits)
    val irq   = out Bool

  }.setName("")

  // All interrupts are asynchronous hence make them synchronous
  val interrupts = BufferCC(io.intsE,
                            init = B(0, noOfInterrupts bits),
                            bufferDepth = 3)

  // Check all interrupts
  io.intNo := OHToUInt(OHMasking.first(io.intsE))

  // Generate a rising edge when an interrupt has happened (init value is false)
  io.irq := interrupts.orR.rise(False)

}
