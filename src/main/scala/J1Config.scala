/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Creation Date:  Tue Nov 15 17:04:09 GMT+1 2016
 * Module Name:    J1Config - Holds a complete CPU configuration
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._
import scala.io.Source

// Configuration of the IRQ controller
case class IRQCtrlConfig (numOfInterrupts         : Int,
                          numOfInternalInterrupts : Int,
                          irqLatency              : Int)

object IRQCtrlConfig {

  // Provide a default configuration
  def default : IRQCtrlConfig = {

    // Default configuration values
    val config = IRQCtrlConfig(numOfInterrupts         = 4,
                               numOfInternalInterrupts = 3,
                               irqLatency              = 3)

    // Return the default configuration
    config

  }

}

// Configuration of timer used for timer interrupts
case class TimerConfig (width : Int)

object TimerConfig {

  // Provide a default configuration
  def default : TimerConfig = {

    // Default configuration values
    val config = TimerConfig(width = 32)

    // Return the default configuration
    config

  }

}

// The configuration of a J1-CPU
case class J1Config (wordSize : Int,
                     dataStackIdxWidth : Int,
                     returnStackIdxWidth : Int,
                     timerConfig : TimerConfig,
                     irqConfig : IRQCtrlConfig,
                     adrWidth : Int,
                     numOfRAMs : Int,
                     startAddress : Int,
                     bootCode : () => List[Bits])

// Holds the configuration parameters of a J1
object J1Config {

  // Some useful instructions
  def instrRTS   = B"0110_0000_1000_1100"
  def instrJMP20 = B"0000_0000_0001_0100"
  def instrJMP50 = B"0000_0000_0011_0010"
  def instrJMP60 = B"0000_0000_0011_1100"

  // Simply halt the CPU (endless loop of instruction at 0)
  def endlessLoop() = List(B"0000_0000_0000_0000") // 0. Jump 0

  // Simple test of the ISA
  def isaTest() = List(B"1000_0000_0000_0111", //  0. Push 7
                       B"1000_0000_0000_0011", //  1. Push 3
                       B"0000_0000_0000_0100", //  2. Jump 4
                       B"1000_0000_0000_0001", //  3. Push 1
                       B"1000_0000_0000_1111", //  4. Push 15
                       B"0110_0010_0000_0011", //  5. Add, drop and push
                       B"0110_0010_0000_0001", //  6. Add and push
                       B"0110_0010_0000_0000", //  7. Add and no push
                       B"0010_0000_0000_1011", //  8. Jump 11 if tos is zero
                       B"1000_0000_0000_0000", //  9. Push 0
                       B"0010_0000_0000_1010", // 10. Jump 10 if tos is zero
                       B"0100_0000_0010_0000", // 11. Call 32
                       B"1000_0000_0000_0001", // 12. Push 1
                       B"0110_0111_0000_0001", // 13. Compare tos and nos push result
                       B"1000_0000_0000_0011", // 14. Push 3
                       B"1000_0000_0000_0011", // 15. Push 3
                       B"0110_0111_0000_0001", // 16. Compare tos and nos push result
                       B"1101_0101_0101_0101", // 17. Push 0x5555
                       B"1000_0000_0100_0000", // 18. Push 0x040
                       B"0110_0000_0100_0011", // 19. ALU I/O operation
                       B"0110_0001_0000_0011", // 20. NOP (wait state for I/O)
                       B"0110_0000_0000_0000", // 21. Clear I/O
                       B"0110_0000_0000_0000", // 22. Clear I/O (wait state)
                       B"1000_0000_0100_0001", // 23. Push 0x41
                       B"1000_0000_1000_0000", // 24. Push 0x080
                       B"0110_0000_0100_0011", // 25. ALU I/O operation
                       B"0110_0001_0000_0011", // 26. NOP (wait state for I/O)
                       B"0110_0000_0000_0000", // 27. Clear I/O
                       B"0110_0000_0000_0000", // 28. Clear I/O
                       B"0000_0000_0100_0110", // 29. Jump 70
                       B"0110_0000_0000_0000", // 30. NOP
                       B"0110_0000_0000_0000", // 31. NOP
                       B"1000_0000_0001_0001", // 32. Push 17
                       B"1000_0000_0001_0000", // 33. Push 16
                       B"0110_1111_0000_0001", // 34. Compare unsigned and push
                       B"1011_1111_1111_1111", // 35. Push +maxint
                       B"1111_1111_1111_1111", // 36. Push -1
                       B"0110_1000_0000_0001", // 37. Compare signed and push
                       B"0110_0001_0000_0011", // 38. Pop
                       B"0110_0001_0000_0011", // 39. Pop
                       B"0110_0001_0000_0011", // 40. Pop
                       B"0110_0001_0000_0011", // 41. Pop
                       B"0110_0001_0000_0011", // 42. Pop
                       B"0110_0001_0000_0011", // 43. Pop
                       B"1000_0000_1111_1110", // 44. Push 254
                       B"1000_0000_1000_0000", // 45. Push 128
                       B"0110_0000_0011_0000", // 46. Write to external RAM
                       B"0110_0001_0000_0011", // 47. Pop
                       B"0110_0001_0000_0011", // 48. Pop
                       B"0110_0000_1000_1100", // 49. Return from Subroutine)
                       B"1010_1010_1010_1000", // 50. Push 0x2AA8 (Interrupt entry point)
                       B"1000_0000_0100_0000", // 51. Push 0x040
                       B"0110_0000_0100_0011", // 52. ALU I/O operation
                       B"0110_0001_0000_0011", // 53. NOP (wait state for I/O)
                       B"0110_0000_0000_0000", // 54. Clear I/O
                       B"0110_0000_0000_0000", // 55. Clear I/O (wait state)
                       B"0110_0001_0000_0011", // 56. Pop
                       B"0110_0001_0000_0011", // 57. Pop
                       B"0110_0000_1000_1100", // 58. Return from subroutine
                       B"0110_0000_0000_0000", // 59. NOP
                       B"1000_0000_0100_0000", // 60. Push LED I/O address 0x40 (Interrupt entry point)
                       B"0110_1101_0101_0000", // 61. Read data from I/O space
                       B"0110_1101_0101_0000", // 62. Read data from I/O space
                       B"0110_0110_0000_0000", // 63. Negate DTOS
                       B"1000_0000_0100_0000", // 64. Push LED I/O address 0x40
                       B"0110_0000_0100_0000", // 65. Write data to I/O space
                       B"0110_0000_0000_0000", // 66. NOP (wait state for I/O)
                       B"0110_0001_0000_0011", // 67. Pop
                       B"0110_0001_0000_0011", // 68. Pop
                       B"0110_0000_1000_1100", // 69. Return from subroutine
                       B"1000_0101_1111_0101", // 70. Push high value for approx 1sec
                       B"1000_0000_1100_0001", // 71. Push I/O address 0xC1
                       B"0110_0000_0100_0000", // 72. ALU I/O operation
                       B"0110_0000_0000_0000", // 73. NOP (wait state for I/O)
                       B"0110_0001_0000_0011", // 74. Pop
                       B"0110_0001_0000_0011", // 75. Pop
                       B"1000_0000_1111_1111", // 76. Push some value
                       B"1000_0000_1100_0010", // 77. Push I/O address 0xC2
                       B"0110_0000_0100_0011", // 78. ALU I/O operation (arbitrary non-zero value starts timer)
                       B"0110_0000_0000_0011", // 79. NOP
                       B"1000_0000_0000_0010", // 80. Push 2 to dstack (to enable interrupts)
                       B"1000_0000_1110_0000", // 81. Push IRQ-controller I/O address 0xE0
                       B"0110_0000_0100_0011", // 82. Write data to I/O space
                       B"0110_0000_0000_0011", // 83. NOP (wait state for I/O)
                       B"0000_0000_0101_0100", // 84. Jump 84
                       B"0110_0000_0000_0000", // 85. NOP
                       B"0110_0000_0000_0000", // 86. NOP
                       B"0110_0000_0000_0000", // 87. NOP
                       B"0110_0000_0000_0000", // 88. NOP
                       B"0110_0000_0000_0000") // 89. NOP

  // Simple test of external memory bus and internal memory
  def ioTest() = List(B"1000_0000_0000_1001", //  0. Push 9
                      B"1000_0000_0000_1011", //  1. Push 11
                      B"1000_0000_0100_0000", //  2. Push 0x40
                      B"0110_0000_0100_0011", //  3. I/O write operation and nip
                      B"0110_0001_0000_0011", //  4. wait state for I/O and pop
                      B"1000_0000_1111_1111", //  5. Push 0xff as a separator
                      B"1000_0000_0100_0000", //  6. Push LED I/O address 0x40
                      B"0110_1101_0101_0000", //  7. Read data from I/O space
                      B"0110_1101_0101_0000", //  8. Read data from I/O space
                      B"1000_0000_1000_0000", //  9. Push address 0x80
                      B"0110_0000_0011_0011", // 10. Write DTOS to memory and nip
                      B"0110_0001_0000_0011", // 11. Pop to clean stack
                      B"1000_0000_1000_0000", // 12. Push address 0x80
                      B"0110_1101_0000_0000", // 13. Read from memory
                      B"0110_0001_0000_0011", // 14. Pop to clean stack
                      B"0000_0000_0000_1100") // 15. Jump 12

  // Simple test of irq logic
  def simpleIRQTest() = List(B"1000_0000_0000_0001", //  0. Push  1
                             B"1000_0000_0000_0010", //  1. Push  2
                             B"1000_0000_0000_0011", //  2. Push  3
                             B"1000_0000_0000_0100", //  3. Push  4
                             B"1000_0000_0000_0101", //  4. Push  5
                             B"1000_0000_0000_0110", //  5. Push  6
                             B"1000_0000_0000_0111", //  6. Push  7
                             B"1000_0000_0000_1000", //  7. Push  8
                             B"1000_0000_0000_1001", //  8. Push  9
                             B"1000_0000_0000_1010", //  9. Push 10
                             B"1000_0000_0000_1011", // 10. Push 11
                             B"1000_0000_0000_1100", // 11. Push 12
                             B"1000_0000_0000_1101", // 12. Push 13
                             B"1000_0000_0000_1110", // 13. Push 14
                             B"1000_0000_0000_1111", // 14. Push 15
                             B"1000_0000_0001_0000", // 15. Push 16
                             B"0000_0000_0001_0000", // 16. Jump 16
                             B"0110_0000_0000_0000", // 17. NOP
                             B"0110_0000_0000_0000", // 18. NOP
                             B"0110_0000_0000_0000", // 19. NOP
                             B"1000_1110_1111_0000", // 20. Push 0x0EF0 (Interrupt entry)
                             B"1000_0000_0000_1110", // 21. Push 0x000E
                             B"0110_0001_0000_0011", // 22. Pop
                             B"0110_0001_0000_0011", // 23. Pop
                             B"0110_0000_1000_1100") // 24. Return from subroutine

  // Convert a bin-string to an integer
  def binToBits(s : String, w : Int) = {B(s.toList.map("01".indexOf(_)).reduceLeft(_ * 2 + _), w bits)}

  // Convert a hex-string to an integer
  def hexToBits(s : String, w : Int) = {B(s.toList.map("0123456789ABCDEF".indexOf(_)).reduceLeft(_ * 16 + _), w bits)}

  // Provide the SwapForth base system
  def forthBase(w : Int) = {

    // Read all lines of the hex dump into a list of strings
    val lines = Source.fromFile("toolchain/forth/build/nuc.binary").getLines().toList.map((s : String) => s.toUpperCase)

    // Only use valid lines (so anything not matching a number is a comment)
    val filteredLines = lines.filter((s : String) => s.matches("[0-9A-F]+"))

    // Convert it to a list of Bits of width w
    filteredLines.map((s : String) => binToBits(s, w))

  }

  // Provide a default configuration
  def default = {

    // Default parameters for a J1 CPU
    def wordSize               = 16
    def dataStackIdxWidth      =  8
    def returnStackIdxWidth    =  4
    def noOfInterrupts         =  8
    def noOfInternalInterrupts =  1
    def irqLatency             =  3
    def adrWidth               = 13
    def numOfRAMs              =  2
    def startAddress           =  0

    // Default timer configuration
    val timerConfig = TimerConfig.default

    // IRQ controller parameters (disable all interrupts by default)
    val irqConfig = IRQCtrlConfig(noOfInterrupts, noOfInternalInterrupts, irqLatency)

    // Relevant content of all generated memory blocks
    def bootCode() = endlessLoop() ++
                     List.fill((1 << adrWidth) - endlessLoop().length)(B(0, wordSize bits))

    // Set the default configuration values
    val config = J1Config(wordSize            = wordSize,
                          dataStackIdxWidth   = dataStackIdxWidth,
                          returnStackIdxWidth = returnStackIdxWidth,
                          timerConfig         = timerConfig,
                          irqConfig           = irqConfig,
                          adrWidth            = adrWidth,
                          numOfRAMs           = numOfRAMs,
                          startAddress        = startAddress,
                          bootCode            = bootCode)

    // Return the default configuration
    config

  }

  // Provide a debug configuration
  def debug = {

    // Parameters of a debug configuration
    def wordSize               = 16
    def dataStackIdxWidth      =  5
    def returnStackIdxWidth    =  4
    def noOfInterrupts         =  4
    def noOfInternalInterrupts =  3
    def irqLatency             =  3
    def adrWidth               = 10
    def numOfRAMs              =  2
    def startAddress           =  0

    // Default timer configuration
    val timerConfig = TimerConfig.default

    // IRQ controller parameters (disable all interrupts by default)
    val irqConfig = IRQCtrlConfig(noOfInterrupts, noOfInternalInterrupts, irqLatency)

    def bootCode() = isaTest() ++
                     List.fill((1 << adrWidth) - isaTest().length - noOfInterrupts)(B(0, wordSize bits)) ++
                     List(instrJMP60) ++
                     List(instrJMP60) ++
                     List(instrJMP60) ++
                     List(instrJMP60)

    // Set the configuration values for ISA debugging
    val config = J1Config(wordSize            = wordSize,
                          dataStackIdxWidth   = dataStackIdxWidth,
                          returnStackIdxWidth = returnStackIdxWidth,
                          timerConfig         = timerConfig,
                          irqConfig           = irqConfig,
                          adrWidth            = adrWidth,
                          numOfRAMs           = numOfRAMs,
                          startAddress        = startAddress,
                          bootCode            = bootCode)

    // Return the default configuration
    config

  }

  // Provide a debug configuration of memory mapped I/O instructions
  def debugIO = {

    def wordSize               = 16
    def dataStackIdxWidth      =  5
    def returnStackIdxWidth    =  4
    def noOfInterrupts         =  4
    def noOfInternalInterrupts =  3
    def irqLatency             =  3
    def adrWidth               = 13
    def numOfRAMs              =  2
    def startAddress           =  0

    // Default timer configuration
    val timerConfig = TimerConfig.default

    // IRQ controller parameters (disable all interrupts by default)
    val irqConfig = IRQCtrlConfig(noOfInterrupts, noOfInternalInterrupts, irqLatency)

    // Relevant content of all generated memories
    def bootCode() = ioTest() ++ List.fill((1 << adrWidth) - ioTest.length)(B(0, wordSize bits))

    // Set the configuration values for debugging I/O instructions
    val config = J1Config(wordSize            = wordSize,
                          dataStackIdxWidth   = dataStackIdxWidth,
                          returnStackIdxWidth = returnStackIdxWidth,
                          timerConfig         = timerConfig,
                          irqConfig           = irqConfig,
                          adrWidth            = adrWidth,
                          numOfRAMs           = numOfRAMs,
                          startAddress        = startAddress,
                          bootCode            = bootCode)

    // Return the default configuration
    config

  }

  // Provide a debug configuration for the interrupt controller
  def debugIRQ = {

    def wordSize               = 16
    def dataStackIdxWidth      =  5
    def returnStackIdxWidth    =  4
    def noOfInterrupts         =  4
    def noOfInternalInterrupts =  3
    def irqLatency             =  3
    def adrWidth               =  9
    def numOfRAMs              =  2
    def startAddress           =  0

    // Default timer configuration
    val timerConfig = TimerConfig.default

    // IRQ controller parameters (enable all interrupts by default)
    val irqConfig = IRQCtrlConfig(noOfInterrupts, noOfInternalInterrupts, irqLatency)

    def bootCode() = simpleIRQTest() ++
                     List.fill((1 << adrWidth) - simpleIRQTest().length - noOfInterrupts)(B(0, wordSize bits)) ++
                     List.fill(1)(instrJMP20) ++
                     List.fill(1)(instrRTS) ++
                     List.fill(1)(instrRTS) ++
                     List.fill(1)(instrRTS)

    // Set the default configuration values
    val config = J1Config(wordSize            = wordSize,
                          dataStackIdxWidth   = dataStackIdxWidth,
                          returnStackIdxWidth = returnStackIdxWidth,
                          timerConfig         = timerConfig,
                          irqConfig           = irqConfig,
                          adrWidth            = adrWidth,
                          numOfRAMs           = numOfRAMs,
                          startAddress        = startAddress,
                          bootCode            = bootCode)

    // Return the default configuration
    config

  }

  // Provide a configuration for SwapForth
  def forth = {

    def wordSize               = 16
    def dataStackIdxWidth      =  5
    def returnStackIdxWidth    =  5
    def noOfInterrupts         =  4
    def noOfInternalInterrupts =  3
    def irqLatency             =  3
    def adrWidth               = 12
    def numOfRAMs              =  2
    def startAddress           =  0

    // Default timer configuration
    val timerConfig = TimerConfig.default

    // IRQ controller parameters (disable all interrupts by default)
    val irqConfig = IRQCtrlConfig(noOfInterrupts, noOfInternalInterrupts, irqLatency)

    // Take the base system and cut the interrupt vectors at the end
    def baseSystem = forthBase(wordSize).take((1 << adrWidth) - noOfInterrupts)

    // Generate the complete memory layout of the system (using invalid interrupt vectors)
    def bootCode() = baseSystem ++ List.fill((1 << adrWidth) - baseSystem.length)(B(0, wordSize bits))

    // Set the configuration values for the forth system
    val config = J1Config(wordSize            = wordSize,
                          dataStackIdxWidth   = dataStackIdxWidth,
                          returnStackIdxWidth = returnStackIdxWidth,
                          timerConfig         = timerConfig,
                          irqConfig           = irqConfig,
                          adrWidth            = adrWidth,
                          numOfRAMs           = numOfRAMs,
                          startAddress        = startAddress,
                          bootCode            = bootCode)

    // Return the default configuration
    config

  }

}
