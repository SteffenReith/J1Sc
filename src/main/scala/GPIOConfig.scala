/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Tue Nov 15 17:04:09 GMT+1 2016
 * Module Name:    GPIOConfig - Holds the configuration of GPIO components
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: 10743e7bebbaa8df9957d4dc3b12aa70c2835144
 * Date: Sat Nov 19 00:31:09 2016 +0100
 */

// Configuration of a LED-bank
case class LEDBankConfig (width : Int,
                          lowActive : Boolean)

object LEDBankConfig {

  // Provide a default configuration
  def default = {

    // Default configuration values
    val config = LEDBankConfig(width = 16,
                               lowActive = false)

    // Return the default configuration
    config

  }

}

// Configuration of all GPIO components
case class GPIOConfig (ledBankConfig  :  LEDBankConfig,
                       gpioWaitStates : Int)

object GPIOConfig {

  // Provide a default configuration
  def default = {

    // Default configuration values
    val config = GPIOConfig(ledBankConfig = LEDBankConfig.default, 1)

    // Return the default configuration
    config

  }

}