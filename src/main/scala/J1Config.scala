/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Tue Nov 15 17:04:09 GMT+1 2016
 * Module Name:    J1Config - Holds a complete CPU configuration
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: <COMMITHASH>
 * Date: <AUTHORDATE>
 */

// The configuration of a J1-CPU
case class J1Config (wordSize : Int,
                     dataStackIdxWidth : Int,
                     returnStackIdxWidth : Int,
                     addrWidth : Int,
                     startAddress : Int)

// Holds the configuration parameters of a J1
object J1Config {

  // Provide a default configuration
  def default = {

    // Default configuration values
    val config = J1Config(wordSize            = 16,
                          dataStackIdxWidth   =  8,
                          returnStackIdxWidth =  6,
                          addrWidth           = 13,
                          startAddress        =  0 )

    // Return the default configuration
    config

  }

  // Provide a debug configuration
  def debug = {

    // Default configuration values
    val config = J1Config(wordSize            = 16,
                          dataStackIdxWidth   =  4,
                          returnStackIdxWidth =  3,
                          addrWidth           =  8,
                          startAddress        =  0 )

    // Return the default configuration
    config

  }

}
