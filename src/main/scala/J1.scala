/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Create Date:    Tue Sep 20 15:07:10 CEST 2016 
 * Module Name:    J1 - Toplevel CPU (Core, Memory)
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._
import spinal.lib._

class J1(cfg : J1Config) extends Component {

  // Internal signals
  val internal = new Bundle {

    // Stall signal of CPU
    val stall = in Bool

    // Interface for the interrupt system
    val irq    = in Bool
    val intVec = in Bits (cfg.adrWidth bits)

  }.setName("")

  // I/O signals for the jtag interface (if needed)
  val jtagCondIOArea = cfg.hasJtag generate new Area {

    // Create the interface bundle
    val jtagMemBus = new Bundle {

      // Signal from jtag for memory bus
      val captureMemory = in Bool
      val jtagMemAdr    = in Bits (cfg.adrWidth bits)
      val jtagMemWord   = in Bits (cfg.wordSize bits)

    }.setName("")

  }

  // Peripheral bus
  val bus = new Bundle {

    // I/O signals for peripheral data port
    val cpuBus = master(J1Bus(cfg))

  }.setName("")

  // Create a new CPU core
  val coreJ1CPU = new J1Core(cfg)

  // Create the main memory
  val mainMem = new MainMemory(cfg)

  // Instruction port (read only)
  mainMem.internal.readDataAdr <> coreJ1CPU.internal.nextInstrAdr
  coreJ1CPU.internal.memInstr  <> mainMem.internal.readData

  // Check if we have a jtag interface
  if (cfg.hasJtag) {

    // Check whether the cpu is stalled (otherwise we have no access to the memory)
    when (internal.stall) {

      // Connect the synchronized hold register of the jtag interface
      mainMem.internal.writeEnable  <> jtagCondIOArea.jtagMemBus.captureMemory
      mainMem.internal.writeDataAdr <> jtagCondIOArea.jtagMemBus.jtagMemAdr.asUInt
      mainMem.internal.writeData    <> jtagCondIOArea.jtagMemBus.jtagMemWord

    }.otherwise {

      // Connect the CPU core with the main memory (convert the byte address to a cell address)
      mainMem.internal.writeEnable  <> coreJ1CPU.internal.memWriteMode
      mainMem.internal.writeDataAdr <> coreJ1CPU.internal.extAdr(cfg.adrWidth downto 1)
      mainMem.internal.writeData    <> coreJ1CPU.internal.extToWrite

    }

  } else {

    // Connect the CPU core with the main memory (convert the byte address to a cell address)
    mainMem.internal.writeEnable  <> coreJ1CPU.internal.memWriteMode
    mainMem.internal.writeDataAdr <> coreJ1CPU.internal.extAdr(cfg.adrWidth downto 1)
    mainMem.internal.writeData    <> coreJ1CPU.internal.extToWrite

  }

  // Check whether data should be read for I/O space else provide a constant zero value
  val coreMemRead = coreJ1CPU.internal.ioReadMode ? bus.cpuBus.readData | B(0, cfg.wordSize bits)

  // Read port of CPU core (multiplexed)
  coreJ1CPU.internal.toRead <> coreMemRead

  // Connect the external bus to the core (remember coreJ1CPU.io.extAdr is one clock too early)
  bus.cpuBus.enable    := coreJ1CPU.internal.ioWriteMode || coreJ1CPU.internal.ioReadMode
  bus.cpuBus.writeMode <> coreJ1CPU.internal.ioWriteMode
  bus.cpuBus.address   <> Delay(coreJ1CPU.internal.extAdr, 1)
  bus.cpuBus.writeData <> coreJ1CPU.internal.extToWrite

  // Connect the stall
  coreJ1CPU.internal.stall <> internal.stall

  // Connect the interrupts
  coreJ1CPU.internal.intVec <> internal.intVec
  coreJ1CPU.internal.irq    <> internal.irq

}
