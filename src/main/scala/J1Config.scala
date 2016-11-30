/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Tue Nov 15 17:04:09 GMT+1 2016
 * Module Name:    J1Config - Holds a complete CPU configuration
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: 0a2e9c9b22e7282083028bf56157ad357d6eaeab
 * Date: Sat Nov 19 00:31:59 2016 +0100
 */

import spinal.core._

// The configuration of a J1-CPU
case class J1Config (wordSize : Int,
                     dataStackIdxWidth : Int,
                     returnStackIdxWidth : Int,
                     noOfInterrupts : Int,
                     noOfInternalInterrupts : Int,
                     addrWidth : Int,
                     startAddress : Int,
                     bootCode : () => List[Bits])

// Holds the configuration parameters of a J1
object J1Config {

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
                       B"1101_0101_0101_0111", // 17. Push 0x5557
                       B"1000_0000_0100_0000", // 18. Push 0x040
                       B"0110_0000_0100_0000", // 19. ALU I/O operation
                       B"0110_0000_0000_0000", // 20. NOP (wait state for I/O)
                       B"0110_0000_0000_0000", // 21. Clear I/O               
                       B"0110_0000_0000_0000", // 22. Clear I/O (wait state)
                       B"1000_0000_0100_0001", // 23. Push 0x41
                       B"1000_0010_1000_0000", // 24. Push 0x080
                       B"0110_0000_0100_0000", // 25. ALU I/O operation
                       B"0110_0000_0000_0000", // 26. NOP (wait state for I/O)
                       B"0110_0000_0000_0000", // 27. Clear I/O
                       B"0110_0000_0000_0000", // 28. Clear I/O
                       B"0000_0000_0001_1101", // 29. Jump 29
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
                       B"0111_0000_0000_1100") // 49. Return from Subroutine)

  // Provide a default configuration
  def default = {

    def wordSize               = 16
    def dataStackIdxWidth      =  8
    def returnStackIdxWidth    =  4
    def noOfInterrupts         =  8
    def noOfInternalInterrupts = 1
    def addrWidth              = 13
    def startAddress           =  0
    def instrRTS               = B"111_0000_0000_1100"

    def bootCode() = endlessLoop() ++
                     List.fill((1 << addrWidth) - endlessLoop().length - noOfInterrupts)(B(0, wordSize bits)) ++
                     List.fill(noOfInterrupts)(instrRTS)

    // Default configuration values
    val config = J1Config(wordSize               = wordSize,
                          dataStackIdxWidth      = dataStackIdxWidth,
                          returnStackIdxWidth    = returnStackIdxWidth,
                          noOfInterrupts         = noOfInterrupts,
                          noOfInternalInterrupts = noOfInternalInterrupts,
                          addrWidth              = addrWidth,
                          startAddress           = startAddress,
                          bootCode               = bootCode)

    // Return the default configuration
    config

  }

  // Provide a debug configuration
  def debug = {

    def wordSize               = 16
    def dataStackIdxWidth      =  5
    def returnStackIdxWidth    =  4
    def noOfInterrupts         =  4
    def noOfInternalInterrupts =  1
    def addrWidth              =  9
    def startAddress           =  0
    def instrRTS               = B"111_0000_0000_1100"

    def bootCode() = isaTest() ++
                     List.fill((1 << addrWidth) - isaTest().length - noOfInterrupts)(B(0, wordSize bits)) ++
                     List.fill(noOfInterrupts)(instrRTS)

    // Default configuration values
    val config = J1Config(wordSize               = wordSize,
                          dataStackIdxWidth      = dataStackIdxWidth,
                          returnStackIdxWidth    = returnStackIdxWidth,
                          noOfInterrupts         = noOfInterrupts,
                          noOfInternalInterrupts = noOfInternalInterrupts,
                          addrWidth              = addrWidth,
                          startAddress           = startAddress,
                          bootCode               = bootCode)

    // Return the default configuration
    config

  }

}
