/*
 * Author: Steffen Reith (steffen.reith@hs-rm.de)
 *
 * Creation Date:  Mon Dec 26 17:41:43 CET 2022
 * Module Name:    ArgConfig - Holds all information after parsing command line parameter s
 * Project Name:   J1Sc   - A simple J1 implementation in Scala using Spinal HDL
 *
 */

// Used by scopt to store information
case class ArgsConfig(useDevicePrefix    : Boolean,
                      createWaveFile     : Boolean,
                      serialDevicePrefix : String,
                      serialDeviceName   : String)
