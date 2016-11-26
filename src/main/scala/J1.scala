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

    // I/O signals for peripheral data port
    val cpuBus = master(SimpleBus(cfg.addrWidth, cfg.wordSize))

  }.setName("")

  // Signals for main memory
  val memWriteEnable = Bool
  val memAdr         = UInt(cfg.addrWidth bits)
  val memWrite       = Bits(cfg.wordSize bits)
  val memRead        = Bits(cfg.wordSize bits)

  // Main memory prefilled with boot code
  val mainMem = Mem(Bits(cfg.wordSize bits),
                    cfg.bootCode() ++ List.fill((1 << cfg.addrWidth) - cfg.bootCode().length)(B(0, cfg.wordSize bits)))

  // Create a new CPU core
  val coreJ1CPU = new J1Core(cfg)

  // Create data port for mainMem
  mainMem.write(enable  = memWriteEnable,
                address = memAdr,
                data    = memWrite)
  memRead := mainMem.readSync(address = memAdr, readUnderWrite = readFirst)

  // Instruction port (read only)
  coreJ1CPU.io.instr := mainMem.readSync(address = coreJ1CPU.io.instrAdr, readUnderWrite = readFirst)

  // connect the CPU core with the internal memory
  memWriteEnable <> coreJ1CPU.io.memWriteMode
  memAdr <> coreJ1CPU.io.extAdr
  memWrite <> coreJ1CPU.io.extToWrite
  coreJ1CPU.io.memToRead <> memRead

  // Create an interrupt controller and connect it
  val intCtrl = new InterruptCtrl(noOfInterrupts = cfg.noOfInterrupts)
  coreJ1CPU.io.intNo <> intCtrl.io.intNo
  coreJ1CPU.io.irq <> intCtrl.io.irq

  // Connect the external bus to the core
  io.cpuBus.enable      := coreJ1CPU.io.ioWriteMode || coreJ1CPU.io.ioReadMode
  io.cpuBus.writeMode   <> coreJ1CPU.io.ioWriteMode
  io.cpuBus.address     <> coreJ1CPU.io.extAdr
  coreJ1CPU.io.ioToRead <> io.cpuBus.readData
  io.cpuBus.writeData   <> coreJ1CPU.io.extToWrite

}
