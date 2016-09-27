/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 * Committer: Steffen Reith
 *
 * Create Date:    Tue Sep 27 10:35:21 CEST 2016 
 * Module Name:    J1Core - Toplevel
 * Project Name:   J1Sc - A simple J1 implementation in scala
 *
 * Hash: <COMMITHASH>
 * Date: <AUTHORDATE>
 */
import spinal.core._
import spinal.lib._

object J1Core {

  // Make the reset synchron
  val globalClockConfig = ClockDomainConfig (
    clockEdge        = RISING,
    resetKind        = SYNC,
    resetActiveLevel = HIGH
  )

  def main(args: Array[String]) {

    // Generate HDL files
    SpinalConfig(genVhdlPkg = false,
                 defaultConfigForClockDomains = globalClockConfig,
                 targetDirectory="gen/src/vhdl").generateVhdl(new J1Core)
    SpinalConfig(genVhdlPkg = false,
                 defaultConfigForClockDomains = globalClockConfig,
                 targetDirectory="gen/src/verilog").generateVerilog(new J1Core)

  }

}

