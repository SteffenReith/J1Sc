/*
 * Author: <AUTHORNAME>
 *
 * Create Date:    Tue Sep 20 15:07:10 CEST 2016 
 * Module Name:    J1 - Toplevel CPU (Core, Memory)
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
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

  // Create a new CPU core
  val coreJ1CPU = new J1Core(cfg)

  // Create the main memory
  val mainMem = new MainMemory(cfg)

  // Instruction port (read only)
  mainMem.io.readDataAdr <> coreJ1CPU.io.nextInstrAdr
  coreJ1CPU.io.memInstr  <> mainMem.io.readData

  // Connect the CPU core with the main memory (convert the byte address to a cell address)
  mainMem.io.writeEnable  <> coreJ1CPU.io.memWriteMode
  mainMem.io.writeDataAdr <> coreJ1CPU.io.extAdr(cfg.adrWidth downto 1)
  mainMem.io.writeData    <> coreJ1CPU.io.extToWrite

  // Check whether data should be read for I/O space else provide a constant zero value
  val coreMemRead = coreJ1CPU.io.ioReadMode ? io.cpuBus.readData | B(0, cfg.wordSize bits)

  // Read port of CPU core (multiplexed)
  coreJ1CPU.io.toRead <> coreMemRead

  // Connect the external bus to the core (remember coreJ1CPU.io.extAdr is one clock too early)
  io.cpuBus.enable    := coreJ1CPU.io.ioWriteMode || coreJ1CPU.io.ioReadMode
  io.cpuBus.writeMode <> coreJ1CPU.io.ioWriteMode
  io.cpuBus.address   <> Delay(coreJ1CPU.io.extAdr, 1)
  io.cpuBus.writeData <> coreJ1CPU.io.extToWrite

  // Connect the interrupts
  coreJ1CPU.io.intNo <> io.intNo
  coreJ1CPU.io.irq   <> io.irq

}
