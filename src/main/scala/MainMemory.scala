/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Sun Jan 22 12:51:03 GMT+1 2017
 * Module Name:    MainMemory - implementation of 64k words main memory
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: 8469f68ac67bc74f6a39e8dd8dd35e6750c1bd48
 * Date: Sun Jan 22 13:10:06 2017 +0100
 */
import spinal.core._

class MainMemory(cfg : J1Config) extends Component {

  // Check the generic parameters
  assert(Bool(cfg.wordSize >= cfg.adrWidth), "Error: The width of an address is too large", FAILURE)

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

  // Create the instruction port (read only) for the instruction memory
  io.memInstr := lowMem.readSync(address = io.memInstrAdr.resized, readUnderWrite = readFirst)

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

  // Create all write ports and the needed enable signals
  val ramIdxList = ramList.zipWithIndex
  ramIdxList.map {case(mem, idx) => mem.write(enable  = io.memWriteEnable &&
                                                        (U(idx) === io.memAdr(cfg.wordSize - 1 downto cfg.adrWidth)),
                                              address = io.memAdr(cfg.adrWidth - 1 downto 0),
                                              data    = io.memWrite)}

  // Create a list of all read ports
  val rPortsList = ramList.map {mem => mem.readSync(address        = io.memAdr(cfg.adrWidth - 1 downto 0),
                                                    readUnderWrite = readFirst)}

  // Convert the list to a spinal vector
  val rPortsVec = Vec(Bits(cfg.wordSize bits), noOfRAMs)

  // Copy the list to the vector
  for(i <- 0 to noOfRAMs - 1) {

    // Copy the ith RAM to the Vec
    rPortsVec(i) := rPortsList(i)

  }

  // Multiplex the output
  io.memRead := rPortsVec(io.memAdr(cfg.wordSize - 1 downto cfg.adrWidth))

}

class MainMemoryOld(cfg : J1Config) extends Component {

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

  // Main memory pre filled with boot code
  val mainMem = Mem(Bits(cfg.wordSize bits), cfg.bootCode())

  // Create the instruction port (read only)
  io.memInstr := mainMem.readSync(address = io.memInstrAdr.resized, readUnderWrite = readFirst)

  // Create write port port for mainMem
  mainMem.write(enable  = io.memWriteEnable,
                address = io.memAdr(cfg.wordSize - 1 downto 0),
                data    = io.memWrite)

  // Read port for main memory
  io.memRead := mainMem.readSync(address = io.memAdr(cfg.wordSize - 1 downto 0), readUnderWrite = readFirst)

}