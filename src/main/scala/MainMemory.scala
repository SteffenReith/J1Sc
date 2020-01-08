/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Creation Date:  Sun Jan 22 12:51:03 GMT+1 2017
 * Module Name:    MainMemory - implementation of 64k words main memory
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Note: This module takes "small" blocks of BRAM and uses Spinal to tie them
 * together. The reason for doing so is the incapability of Xilinx Vivado 2016.4
 * the generate a dual ported BRAM bigger than 64K (one port one gives 0 as
 * a value back). Warning: The simulation works, but the real hardware causes
 * pain
 *
 */
import spinal.core._

class MainMemory(cfg : J1Config) extends Component {

  // Check the generic parameters
  assert(cfg.wordSize >= cfg.adrWidth, message = "ERROR: The width of addresses are too large")
  assert(isPow2(cfg.numOfRAMs), message = "ERROR: Number of RAMs has to be a power of 2")
  assert(cfg.numOfRAMs >= 2, message = "ERROR: Number of RAMs has to be at least 2")

  // I/O ports
  val internal = new Bundle {

    // Instruction port (read only)
    val readDataAdr = in  UInt (cfg.adrWidth bits)
    val readData    = out Bits (cfg.wordSize bits)

    // Memory port (write only)
    val writeEnable  = in Bool
    val writeDataAdr = in UInt (cfg.adrWidth bits)
    val writeData    = in Bits (cfg.wordSize bits)

  }.setName("")

  // Calculate the number of bits needed to address the RAMs
  def ramAdrWidth = log2Up(cfg.numOfRAMs)

  // Number of cells of a RAM
  def numOfCells = 1 << (cfg.adrWidth - ramAdrWidth)

  // Give some information about RAM-size and used bank layout
  println("[J1Sc] Create " + cfg.numOfRAMs + " RAMs which have " + numOfCells + " cells each")
  println("[J1Sc] Total size is " + cfg.numOfRAMs * numOfCells + " cells")

  // Create a complete list of memory blocks (start with first block)
  val ramList = for (i <- 0 until cfg.numOfRAMs) yield {

    // Write a message
    println("[J1Sc]   Filling RAM " +
            i +
            " (Range from " +
            (i * numOfCells) +
            " to " +
            (i * numOfCells + numOfCells - 1) +
            ")")

    // Create the ith RAM and fill it with the appropriate part of the bootcode
    Mem(Bits(cfg.wordSize bits), cfg.bootCode().slice(i * numOfCells, (i + 1) * numOfCells))

  }

  // Convert the list to a spinal vector
  val rPortsVec = Vec(for((ram,i) <- ramList.zipWithIndex) yield {

    // Create the write port of the ith RAM
    ram.write(enable  = internal.writeEnable &&
                        (U(i) === internal.writeDataAdr(internal.writeDataAdr.high downto
                                                        internal.writeDataAdr.high - ramAdrWidth + 1)),
              address = internal.writeDataAdr((internal.writeDataAdr.high - ramAdrWidth) downto 0),
              data    = internal.writeData)

    // Create the read port of the ith RAM
    ram.readSync(address = internal.readDataAdr((internal.readDataAdr.high - ramAdrWidth) downto 0),
                                                readUnderWrite = readFirst)

  })

  // Multiplex the read port
  internal.readData := rPortsVec(RegNext(internal.readDataAdr(internal.readDataAdr.high downto
                                                              internal.readDataAdr.high - ramAdrWidth + 1)))

}
