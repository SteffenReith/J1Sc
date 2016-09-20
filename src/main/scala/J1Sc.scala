/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Create Date:    Tue Sep 20 15:07:10 CEST 2016 
 * Module Name:    J1Sc - Toplevel
 * Project Name:   J1Sc - A simple J1 implementation
 *
 * Hash: <COMMITHASH>
 * Date: <AUTHORDATE>
 */
import spinal.core._
import spinal.lib._

class J1Sc (wordSize : Int = 32) extents Component {

  // I/O ports
  val io = new Bundle {

  }.setName("")

}

object J1Sc {

  // Make the reset synchron
  val globalClockConfig = ClockDomainConfig (
    clockEdge        = RISING,
    resetKind        = SYNC,
    resetActiveLevel = HIGH
  )

  def main(args: Array[String]) {

    // Generate HDL files
    SpinalConfig(genVhdlPkg = false,
                 defaultConfigForClockDomains = globalClockConfig).generateVhdl(new J1Sc)
    SpinalVerilog(new J1Sc)

  }

}
