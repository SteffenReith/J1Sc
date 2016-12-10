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

import spinal.lib.com.uart._

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

// Configuration of timer used for timer interrupts
case class TimerConfig (width : Int)

object TimerConfig {

  // Provide a default configuration
  def default = {

    // Default configuration values
    val config = TimerConfig(width = 32)

    // Return the default configuration
    config

  }

}

// Configuration of the UART
case class J1UARTConfig (clockDividerWidth : Int,
                         dataWidthMax : Int,
                         baudrate : Int,
                         dataLength : Int,
                         parity : UartParityType.E,
                         stop : UartStopType.E,
                         preSamplingSize : Int,
                         samplingSize : Int,
                         postSamplingSize : Int,
                         fifoDepth : Int)

object J1UARTConfig {

  // Provide a default configuration
  def default = J1UARTConfig(clockDividerWidth = 20,
                             dataWidthMax = 8,
                             baudrate = 115200,
                             dataLength = 7,
                             parity = UartParityType.NONE,
                             stop = UartStopType.ONE,
                             preSamplingSize = 1,
                             samplingSize = 5,
                             postSamplingSize = 2,
                             fifoDepth = 8)

  // Return the default configuration
  default

}

// Configuration of all GPIO components
case class GPIOConfig (ledBankConfig  : LEDBankConfig,
                       timerConfig    : TimerConfig,
                       uartConfig     : J1UARTConfig,
                       gpioWaitStates : Int)

object GPIOConfig {

  // Provide a default configuration
  def default = {

    // Default configuration values
    val config = GPIOConfig(ledBankConfig = LEDBankConfig.default,
                            timerConfig   = TimerConfig.default,
                            uartConfig    = J1UARTConfig.default,
                            1)

    // Return the default configuration
    config

  }

}