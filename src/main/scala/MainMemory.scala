/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
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
 * Hash: 8469f68ac67bc74f6a39e8dd8dd35e6750c1bd48
 * Date: Sun Jan 22 13:10:06 2017 +0100
 */
import spinal.core._

class MainMemory(cfg : J1Config) extends Component {

  // Check the generic parameters
  assert(Bool(cfg.wordSize >= cfg.adrWidth), "Error: The width of addresses are too large", FAILURE)

  // I/O ports
  val io = new Bundle {

    // Instruction port
    val memInstrAdr = in UInt(cfg.adrWidth bits)
    val memInstr    = out Bits(cfg.wordSize bits)

    // Memory port
    val memWriteEnable = in Bool
    val memAdr         = in UInt(cfg.wordSize bits)
    val memWrite       = in Bits(cfg.wordSize bits)
    val memRead        = out Bits(cfg.wordSize bits)

  }.setName("")

  // Generate a list holding the lowest memory block (holding the instructions to be executed)
  val lowMem = Mem(Bits(cfg.wordSize bits), cfg.bootCode())

  // Create a read-only port for the instructions (J1 has Harvard-style but shares the data/instruction - memory)
  io.memInstr := lowMem.readSync(address = io.memInstrAdr, readUnderWrite = readFirst)

  // Calculate the number of needed rams
  def noOfRAMs = (1 << (cfg.wordSize - cfg.adrWidth))

  // Holds a complete list of memory blocks (start with first block)
  val ramList = if (noOfRAMs >= 1) {

    // Add the additional memory blocks into a list
    List(lowMem) ++ List.fill(noOfRAMs - 1)(Mem(Bits(cfg.wordSize bits), 1 << cfg.adrWidth))

  } else {

    // We have only one memory block
    List(lowMem)

  }

  // Convert the list to a spinal vector
  val rPortsVec = Vec(for((ram,i) <- ramList.zipWithIndex) yield {

    // Create the write port of the ith RAM
    ram.write(enable  = io.memWriteEnable &&
                               (U(i) === io.memAdr(cfg.wordSize - 1 downto cfg.adrWidth)),
                     address = io.memAdr(cfg.adrWidth - 1 downto 0),
                     data    = io.memWrite)

    // Create the read port of the ith RAM
    ram.readSync(address = io.memAdr(cfg.adrWidth - 1 downto 0),
                 readUnderWrite = readFirst)

  })

  // Multiplex the output
  io.memRead := rPortsVec(RegNext(io.memAdr(cfg.wordSize - 1 downto cfg.adrWidth)))

}
