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

class InterruptCtrl(width : Int) extends Component {

  var io = new Bundle {

    val intsE = in Bits(width bits)

    val intNo = out UInt(log2Up(width) bits)
    val irq   = out Bool

  }.setName("")

  // All interrupts are asynchron hence make them synchron
  val interrupts = BufferCC(io.intsE)

  // Check all interrupts
  for(i <- (width - 1) to 0 by -1) {

    // Check if ith interrupt is active
    if (interrupts(i) == True) io.intNo := i

  }

  // Generate a rising edge when an interrupt has happened (init value is false)
  io.irq := interrupts.orR.rise(False)

}
