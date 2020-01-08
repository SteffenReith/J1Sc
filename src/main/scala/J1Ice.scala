/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Creation Date:  Sun Nov 19 14:08:53 GMT+1 2017 
 * Module Name:    J1SoC - A small but complete system based on the J1-core
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._
import spinal.lib._
import spinal.lib.com.uart._
import spinal.lib.io._

class J1Ice(j1Cfg    : J1Config,
            boardCfg : CoreConfig) extends Component {

  val io = new Bundle {

    // Asynchron reset
    val reset = in Bool

    // A board clock
    val boardClk       = in Bool
    val boardClkLocked = in Bool

    // Asynchronous interrupts from the outside world
    val extInt = in Bits (j1Cfg.irqConfig.numOfInterrupts - j1Cfg.irqConfig.numOfInternalInterrupts bits)

    // The physical pins for the connected LEDs
    val leds = out Bits(boardCfg.ledBankConfig.width bits)

    // The physical pins for the LEDs that support PWM
    val pwmLeds = out Bits(boardCfg.pwmConfig.numOfChannels bits)

    // The physical pins for pmod A
    val pmodA = master(TriStateArray(boardCfg.gpioConfig.width bits))

    // The physical pins for the push buttons
    val pButtons  = in Bits(boardCfg.pButtonConfig.numOfPins bits)

    // I/O pins for the UART
    val rx = in  Bool // UART input
    val tx = out Bool // UART output

    // led for indicating UART activity
    val uartLed = out Bool

  }.setName("")

  // I/O signals for the jtag interface (if needed)
  val jtagCondIOArea = j1Cfg.hasJtag generate new Area {

    // Create the interface bundle
    val jtag = new Bundle {

      // JTAG data input
      val tdi = in Bool

      // JTAG data output
      val tdo = out Bool

      // Control for the JTAG TAP
      val tms = in Bool

      // The JTAG clock (the signal tdi, tdo and tms are synchronous to this clock)
      val tck = in Bool

    }.setName("")

  }

  // Check whether we need a jtag interface
  val jtagIface = j1Cfg.hasJtag generate new Area {

    // Make the reset asynchronous and use the rising edge
    val jtagClockConfig = ClockDomainConfig(clockEdge        = RISING,
                                            resetKind        = ASYNC,
                                            resetActiveLevel = HIGH)

    // Create a clockdomain which is synchron to tck but the global reset is asynchronous to this clock domain
    val jtagClockDomain = ClockDomain(config = jtagClockConfig,
                                      clock  = jtagCondIOArea.jtag.tck,
                                      reset  = io.reset)

    // Create the clock area used for the JTAG
    val jtagArea = new ClockingArea(jtagClockDomain) {

      // Create a JTAG interface
      val jtag = new J1Jtag(j1Cfg, j1Cfg.jtagConfig)

      // Connect the jtag interface
      jtag.io.tdi             <> jtagCondIOArea.jtag.tdi
      jtagCondIOArea.jtag.tdo <> jtag.io.tdo
      jtag.io.tms             <> jtagCondIOArea.jtag.tms

    }

  }

  // Physical clock area (connected to a physical clock generator (e.g. crystal oscillator))
  val clkCtrl = new Area {

    // Create a clock domain which is related to the synthesized clock
    val coreClockDomain = ClockDomain.internal(name = "core", frequency = boardCfg.coreFrequency)

    // Connect the synthesized clock
    coreClockDomain.clock := io.boardClk

    // Check if we have a jtag interface
    if (j1Cfg.hasJtag) {

      // Connect the new asynchronous reset
      coreClockDomain.reset := coreClockDomain(RegNext(ResetCtrl.asyncAssertSyncDeassert(

        // Hold the reset as long as the PLL is not locked
        input       = io.reset || jtagIface.jtagArea.jtag.asyncSignals.jtagReset || (!io.boardClkLocked),
        clockDomain = coreClockDomain

      )))

    } else {

      // Connect the new asynchronous reset
      coreClockDomain.reset := coreClockDomain(RegNext(ResetCtrl.asyncAssertSyncDeassert(

        // Hold the reset as long as the PLL is not locked
        input       = io.reset || (!io.boardClkLocked),
        clockDomain = coreClockDomain

      )))

    }

  }

  // Generate the application specific clocking area
  val coreArea = new ClockingArea(clkCtrl.coreClockDomain) {

    // Give some info about the frequency
    println(s"[J1Sc] Use board frequency of ${ClockDomain.current.frequency.getValue.toBigDecimal /1000000} Mhz")

    // Create a new CPU core
    val cpu = new J1(j1Cfg)

    // Check if we have a jtag interface
    if (j1Cfg.hasJtag) {

      // Connect the jtag halt signal and do a clock domain crossing
      cpu.internal.stall := BufferCC(jtagIface.jtagArea.jtag.internal.jtagStall)

      // Connect the jtag cpu memory signals and do a clock domain crossing
      cpu.jtagCondIOArea.jtagMemBus.captureMemory := BufferCC(jtagIface.jtagArea.jtag.internal.jtagCaptureMemory)
      cpu.jtagCondIOArea.jtagMemBus.jtagMemAdr    := BufferCC(jtagIface.jtagArea.jtag.internal.jtagCPUAdr)
      cpu.jtagCondIOArea.jtagMemBus.jtagMemWord   := BufferCC(jtagIface.jtagArea.jtag.internal.jtagCPUWord)

    } else {

      // Without JTAG deactivate the stall signal
      cpu.internal.stall := False

    }

    // Create a delayed version of the cpu core interface to IO-peripherals
    val peripheralBus     = cpu.bus.cpuBus.delayIt(boardCfg.ioWaitStates)
    val peripheralBusCtrl = J1BusSlaveFactory(peripheralBus)

    // Create a LED array at base address 0x40
    val ledArray  = new LEDArray(j1Cfg, boardCfg.ledBankConfig)
    val ledBridge = ledArray.driveFrom(peripheralBusCtrl, baseAddress = 0x40)

    // Connect the physical LED pins to the outside world
    io.leds := ledArray.io.leds

    // Create the PWMs fpr the RGB-leds at 0x50
    val pwm = new PWM(j1Cfg, boardCfg.pwmConfig)
    val pwmBridge = pwm.driveFrom(peripheralBusCtrl, baseAddress = 0x50)

    // Connect the pwm channels physically
    io.pwmLeds := pwm.io.pwmChannels

    // Create a PMOD at base address 0x60
    val pmodA       = new GPIO(boardCfg.gpioConfig)
    val pmodABridge = pmodA.driveFrom(peripheralBusCtrl, baseAddress = 0x70)

    // Connect the gpio register to pmodA
    io.pmodA.write       <> pmodA.io.dataOut
    pmodA.io.dataIn      <> io.pmodA.read
    io.pmodA.writeEnable <> pmodA.io.directions

    // Create the push button array
    val pButtons = new DBPinArray(j1Cfg, boardCfg.pButtonConfig)
    val pButtonsBridge = pButtons.driveFrom(peripheralBusCtrl, baseAddress = 0x90)
    pButtons.io.inputPins := io.pButtons

    // Tell Spinal that some unneeded signals are allowed to be pruned to avoid warnings
    pButtons.timeOut.stateRise.allowPruning()

    // Create two timer and map it at 0xC0 and 0xD0
    val timerA       = new Timer(j1Cfg.timerConfig)
    val timerABridge = timerA.driveFrom(peripheralBusCtrl, baseAddress = 0xC0)
    val timerB       = new Timer(j1Cfg.timerConfig)
    val timerBBridge = timerB.driveFrom(peripheralBusCtrl, baseAddress = 0xD0)

    // Create an UART interface with fixed capabilities
    val uartCtrlGenerics = UartCtrlGenerics(dataWidthMax      = boardCfg.uartConfig.dataWidthMax,
                                            clockDividerWidth = boardCfg.uartConfig.clockDividerWidth,
                                            preSamplingSize   = boardCfg.uartConfig.preSamplingSize,
                                            samplingSize      = boardCfg.uartConfig.samplingSize,
                                            postSamplingSize  = boardCfg.uartConfig.postSamplingSize)
    val uartCtrlInitConfig = UartCtrlInitConfig(baudrate   = boardCfg.uartConfig.baudrate,
                                                dataLength = boardCfg.uartConfig.dataLength,
                                                parity     = boardCfg.uartConfig.parity,
                                                stop       = boardCfg.uartConfig.stop)
    val uartMemMapConfig = UartCtrlMemoryMappedConfig(uartCtrlConfig                = uartCtrlGenerics,
                                                      initConfig                    = uartCtrlInitConfig,
                                                      busCanWriteClockDividerConfig = false,
                                                      busCanWriteFrameConfig        = false,
                                                      txFifoDepth                   = boardCfg.uartConfig.fifoDepth,
                                                      rxFifoDepth                   = boardCfg.uartConfig.fifoDepth)
    val uartCtrl = new UartCtrl(uartCtrlGenerics)

    // Map the UART to 0xF0 and enable the generation of read/write interrupts
    val uartBridge = uartCtrl.driveFrom(peripheralBusCtrl, uartMemMapConfig, baseAddress = 0xF0)
    uartBridge.interruptCtrl.readIntEnable := True

    // Tell Spinal that some unneeded signals are allowed to be pruned to avoid warnings
    uartBridge.interruptCtrl.interrupt.allowPruning()

    // Connect the physical UART pins to the outside world
    io.tx := uartCtrl.io.uart.txd
    uartCtrl.io.uart.rxd := io.rx

    // Create a timeout of the UART led
    val uartTimeOut = Timeout(50 ms)

    // Start timeout when the UART becomes active (note that high indicates inactivity)
    when(!(io.tx && io.rx)) {

      // Reset the write timeout
      uartTimeOut.clear()

    }

    // Handle the led for UART activity (suppose that the led is low active)
    io.uartLed := uartTimeOut.state

    // Create an interrupt controller, map it to 0xE0 and connect all interrupts
    val intCtrl = new InterruptCtrl(j1Cfg)
    val intCtrlBridge = intCtrl.driveFrom(peripheralBusCtrl, baseAddress = 0xE0)
    intCtrl.io.irqReqs(intCtrl.io.irqReqs.high downto j1Cfg.irqConfig.numOfInternalInterrupts) <> io.extInt
    intCtrl.io.irqReqs(0) <> uartBridge.interruptCtrl.readInt
    intCtrl.io.irqReqs(1) <> timerA.io.interrupt
    intCtrl.io.irqReqs(2) <> timerB.io.interrupt
    cpu.internal.intVec <> intCtrl.internal.intVec.resize(j1Cfg.adrWidth)
    cpu.internal.irq <> intCtrl.internal.irq

  }

}

object J1Ice {

  // Make the reset synchron and use the rising edge
  val globalClockConfig = ClockDomainConfig(clockEdge        = RISING,
                                            resetKind        = SYNC,
                                            resetActiveLevel = HIGH)

  def main(args : Array[String]) {

    def elaborate = {

      // Configuration of CPU-core
      val j1Cfg = J1Config.forth16

      // Configuration of the ICEBreaker board
      val boardCfg = CoreConfig.iceBoard

      // Create a system instance
      new J1Ice(j1Cfg, boardCfg)

    }

    // Write a message
    println("[J1Sc] Create the J1 for an IceBreaker board using")

    // Generate VHDL code (for debugging purposes)
    SpinalConfig(mergeAsyncProcess = true,
                 genVhdlPkg = true,
                 defaultConfigForClockDomains = globalClockConfig,
                 targetDirectory="gen/src/vhdl").generateVhdl(elaborate).printPruned()

    // Generate Verilog / Maybe mergeAsyncProcess = false helps verilator to avoid wrongly detected combinatorial loops
    SpinalConfig(mergeAsyncProcess = true,
                 defaultConfigForClockDomains = globalClockConfig,
                 targetDirectory="gen/src/verilog").generateVerilog(elaborate).printPruned()

  }

}
