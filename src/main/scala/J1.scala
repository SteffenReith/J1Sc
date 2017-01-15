/*
 * Author: <AUTHORNAME>
 * Committer: <COMMITTERNAME>
 *
 * Create Date:    Tue Sep 20 15:07:10 CEST 2016 
 * Module Name:    J1 - Toplevel CPU (Core, Memory)
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: 0a82e2997654f501bf8937447a1521127602ea03
 * Date: Tue Nov 1 15:35:55 2016 +0100
 */
import spinal.core._
import spinal.lib._

class J1(cfg : J1Config) extends Component {

  // I/O ports
  val io = new Bundle {

    // Interface for the interrupt system
    val irq   = in Bool
    val intNo = in UInt(log2Up(cfg.irqConfig.numOfInterrupts) bits)

    // I/O signals for peripheral data port
    val cpuBus = master(SimpleBus(cfg))

  }.setName("")

  // Signals for main memory
  val memWriteEnable = Bool
  val memAdr         = UInt(cfg.adrWidth bits)
  val memWrite       = Bits(cfg.wordSize bits)
  val memRead        = Bits(cfg.wordSize bits)

  // Main memory pre filled with boot code
  val mainMem = Mem(Bits(cfg.wordSize bits), cfg.bootCode())

  // Create a new CPU core
  val coreJ1CPU = new J1Core(cfg)

  // Create data port for mainMem
  mainMem.write(enable  = memWriteEnable,
                address = memAdr,
                data    = memWrite)
  memRead := mainMem.readSync(address = memAdr, readUnderWrite = readFirst)

  // Instruction port (read only)
  coreJ1CPU.io.memInstr := mainMem.readSync(address = coreJ1CPU.io.instrAdr, readUnderWrite = readFirst)

  // connect the CPU core with the internal memory
  memWriteEnable <> coreJ1CPU.io.memWriteMode
  memAdr <> coreJ1CPU.io.extAdr(cfg.adrWidth - 1 downto 0)
  memWrite <> coreJ1CPU.io.extToWrite
  coreJ1CPU.io.memToRead <> memRead

  // Connect the external bus to the core (remember coreJ1CPU.io.extAdr is one clock too early)
  io.cpuBus.enable      := coreJ1CPU.io.ioWriteMode || coreJ1CPU.io.ioReadMode
  io.cpuBus.writeMode   <> coreJ1CPU.io.ioWriteMode
  io.cpuBus.address     <> Delay(coreJ1CPU.io.extAdr, 1)
  coreJ1CPU.io.ioToRead <> io.cpuBus.readData
  io.cpuBus.writeData   <> coreJ1CPU.io.extToWrite

  // Connect the interrupts
  coreJ1CPU.io.intNo <> io.intNo
  coreJ1CPU.io.irq <> io.irq

}
