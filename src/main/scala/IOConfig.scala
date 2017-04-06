/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Tue Nov 15 17:04:09 GMT+1 2016
 * Module Name:    IOConfig - Holds the configuration of external IO components
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: <COMMITHASH>
 * Date: <AUTHORDATE>
 */
import spinal.lib.com.uart._

// Configuration of a PMod-interface
case class GPIOConfig(width : Int)

object GPIOConfig {

  // Provide a default configuration
  def default = {

    // Default configuration values
    val config = GPIOConfig(width = 8)

    // Return the default configuration
    config

  }

}

// Configuration of a LED-array
case class LEDArrayConfig(width : Int, lowActive : Boolean)

object LEDArrayConfig {

  // Provide a default configuration
  def default = {

    // Default configuration values
    val config = LEDArrayConfig(width = 16,
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
  def default = {

    val config = J1UARTConfig(clockDividerWidth = 20,
                              dataWidthMax = 8,
                              baudrate = 115200,
                              dataLength = 7,
                              parity = UartParityType.NONE,
                              stop = UartStopType.ONE,
                              preSamplingSize = 1,
                              samplingSize = 5,
                              postSamplingSize = 2,
                              fifoDepth = 8)

    // Return the configuration
    config

  }

  // Provide a configuration for SwapForth
  def forth = {

    val config = J1UARTConfig(clockDividerWidth = 20,
                              dataWidthMax = 8,
                              baudrate = 8 * 115200,
                              dataLength = 7,
                              parity = UartParityType.NONE,
                              stop = UartStopType.ONE,
                              preSamplingSize = 1,
                              samplingSize = 5,
                              postSamplingSize = 2,
                              fifoDepth = 8)

    // Return the forth configuration
    config

  }

}

// Configuration of all IO components
case class IOConfig(pmodConfig     : GPIOConfig,
                    ledBankConfig  : LEDArrayConfig,
                    timerConfig    : TimerConfig,
                    uartConfig     : J1UARTConfig,
                    gpioWaitStates : Int)

object IOConfig {

  // Provide a default configuration
  def default = {

    // Default configuration values
    val config = IOConfig(pmodConfig      = GPIOConfig.default,
                            ledBankConfig = LEDArrayConfig.default,
                            timerConfig   = TimerConfig.default,
                            uartConfig    = J1UARTConfig.default,
                            1)

    // Return the default configuration
    config

  }

  // Provide a configuration for SwapForth
  def forth = {

    // Default configuration values
    val config = IOConfig(pmodConfig      = GPIOConfig.default,
                            ledBankConfig = LEDArrayConfig.default,
                            timerConfig   = TimerConfig.default,
                            uartConfig    = J1UARTConfig.forth,
                            1)

    // Return the forth configuration
    config

  }

}
