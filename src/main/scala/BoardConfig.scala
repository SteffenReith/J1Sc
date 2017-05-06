/*
 * Author: Steffen Reith (steffen.reith@hs-rm.de)
 *
 * Creation Date:  Tue Nov 15 17:04:09 GMT+1 2016
 * Module Name:    BoardConfig - Holds the configuration of the used (development) board
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.lib.com.uart._
import spinal.core._

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

// Information about controllable LEDs on the board
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

  // Provide a configuration for the Nexys4DDR board from Digilent
  def nexys4DDRUartConfig = {

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

    // Return the Nexys4DDR configuration
    config

  }

}

// Configuration of all IO components
case class BoardConfig(gpioConfig     : GPIOConfig,
                       ledBankConfig  : LEDArrayConfig,
                       uartConfig     : J1UARTConfig,
                       boardFrequency : IClockDomainFrequency,
                       ioWaitStates   : Int)

object BoardConfig {

  // Provide a default configuration
  def default = {

    // Default configuration values
    val config = BoardConfig(gpioConfig     = GPIOConfig.default,
                             ledBankConfig  = LEDArrayConfig.default,
                             uartConfig     = J1UARTConfig.default,
                             boardFrequency = FixedFrequency(80 MHz),
                             1)

    // Return the default configuration
    config

  }

  // Provide a configuration for the Nexys4DDR board from Digilent
  def nexys4DDR = {

    // Default configuration values
    val config = BoardConfig(gpioConfig     = GPIOConfig.default,
                             ledBankConfig  = LEDArrayConfig.default,
                             uartConfig     = J1UARTConfig.nexys4DDRUartConfig,
                             boardFrequency = FixedFrequency(100 MHz),
                             1)

    // Return the Nexys4DDR configuration
    config

  }

  // Provide a configuration for the Nexys4 board from Digilent
  def nexys4 = {

    // Default configuration values
    val config = BoardConfig(gpioConfig     = GPIOConfig.default,
      ledBankConfig  = LEDArrayConfig.default,
      uartConfig     = J1UARTConfig.nexys4DDRUartConfig,
      boardFrequency = FixedFrequency(100 MHz),
      1)

    // Return the Nexys4 configuration
    config

  }

}
