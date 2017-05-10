/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de) 
 *
 * Creation Date:  Fri Nov 25 13:04:23 GMT+1 2016
 * Module Name:    InterruptCtrl - A small interrupt controller for the J1 CPU
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._
import spinal.lib._
import spinal.lib.bus.misc.BusSlaveFactory

class InterruptCtrl(cfg : J1Config) extends Component {

  // Check the number of interrupts
  assert(isPow2(cfg.irqConfig.numOfInterrupts), "Warning: Specify a power of 2 as number of interrupts")

  var io = new Bundle {

    val accessMaskWrite = in Bool

    val irqMask      = in Bits(cfg.wordSize bits)
    val irqMaskState = out Bits(cfg.wordSize bits)

    val intsE = in Bits(cfg.irqConfig.numOfInterrupts bits)

    val intNo = out UInt(log2Up(cfg.irqConfig.numOfInterrupts) bits)
    val irq   = out Bool

  }.setName("")

  // Register the irq mask (adapt the width for the 1 bit register)
  val maskNotAllZero : Bool = io.irqMask.orR
  val isEnabled = RegNextWhen(maskNotAllZero, io.accessMaskWrite)

  // Check parameter which indicates whether the interrupts are active after reset
  if (cfg.irqConfig.interruptsDefaultActive) {

    // Enable all interrupts after reset
    isEnabled.init(True)

  } else {

    // Disable all interrupts after reset
    isEnabled.init(False)

  }

  // Generate an output version of the current mask
  io.irqMaskState := (default -> isEnabled)

  // All interrupts are asynchronous, hence make them synchronous
  val interrupts = BufferCC(io.intsE,
                            init = B(0, cfg.irqConfig.numOfInterrupts bits),
                            bufferDepth = 3)

  // Check all interrupts (priority from 0 (high) to noOfInterrupts - 1 (low))
  io.intNo := OHToUInt(OHMasking.first(interrupts))

  // Generate a rising edge when an interrupt has happened (init value is false) and the interrupts are enabled
  io.irq := interrupts.orR.rise(False) & isEnabled

  // Implement the bus interface
  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : BigInt) = new Area {

    // A r/w register for enabling the timer (anything != 0 means true)
    busCtrl.read(io.irqMaskState, baseAddress + 0, 0)
    busCtrl.nonStopWrite(io.irqMask, 0) // the enable signal is constantly driven by the data of the memory bus

    // Generate the write enable signal for the interrupt mask
    io.accessMaskWrite := busCtrl.isWriting(baseAddress + 0)

  }

}
