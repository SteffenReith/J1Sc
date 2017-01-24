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
