/*
 * Author: Steffen Reith (steffen.reith@hs-rm.de)
 *
 * Creation Date:  Sun Dec 25 13:38:20 CET 2022
 * Module Name:    SimConfig - Holds the configuration used for the simulation
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */

// Information related to the simulation
case class J1SimConfig(devicePrefix : String, serialDeviceName : String, simTimeRes : Double, simCycles : Long)

object J1SimConfig
{
  def default : J1SimConfig = {

    // Provide a default configuration
    val config = J1SimConfig(devicePrefix     = "/dev",
                             serialDeviceName = "tnt1",
                             simTimeRes       = 1e9,
                             simCycles        = 10000000L)

    config

  }

}