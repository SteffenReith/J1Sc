/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 *
 * Creation Date:  Tue Nov 1 14:34:09 GMT+1 2016
 * Module Name:    J1SoC - A small but complete system based on the J1-core
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._
import spinal.lib._
import spinal.lib.com.uart._
import spinal.lib.io._

class J1SoC (j1Cfg    : J1Config,
             boardCfg : BoardConfig) extends Component {

  val io = new Bundle {

    // Asynchron reset
    val reset = in Bool

    // A board clock
    val boardClk       = in Bool
    val boardClkLocked = in Bool

    // Asynchronous interrupts for the outside world
    val extInt = in Bits (j1Cfg.irqConfig.numOfInterrupts - j1Cfg.irqConfig.numOfInternalInterrupts bits)

    // The physical pins for the connected FPGAs
    val leds = out Bits(boardCfg.ledBankConfig.width bits)

    // The physical pins for pmod A
    val pmodA = master(TriStateArray(boardCfg.gpioConfig.width bits))

    // I/O pins for the UART
    val rx =  in Bool // UART input
    val tx = out Bool // UART output

  }.setName("")

  // Physical clock area (connected to a physical clock generator (e.g. crystal oscillator))
  val clkCtrl = new Area {

    // Create a clock domain which is related to the synthesized clock
    val coreClockDomain = ClockDomain.internal("core", frequency = FixedFrequency(80 MHz))

    // Connect the synthesized clock
    coreClockDomain.clock := io.boardClk

    // Connect the new asynchron reset
    coreClockDomain.reset := coreClockDomain(RegNext(ResetCtrl.asyncAssertSyncDeassert(

      // Hold the reset as long as the PLL is not locked
      input = io.reset || ! io.boardClkLocked,
      clockDomain = coreClockDomain

    )))

  }

  // Generate the application specific clocking area
  val coreArea = new ClockingArea(clkCtrl.coreClockDomain) {

    // Create a new CPU core
    val cpu = new J1(j1Cfg)

    // Create a delayed version of the cpu core interface to IO-peripherals
    val peripheralBus = cpu.io.cpuBus.delayed(boardCfg.ioWaitStates)
    val peripheralBusCtrl = SimpleBusSlaveFactory(peripheralBus)

    // Create a LED array at base address 0x40
    val ledArray = new LEDArray(boardCfg.ledBankConfig)
    val ledBridge = ledArray.driveFrom(peripheralBusCtrl, 0x40)

    // Connect the physical LED pins to the outside world
    io.leds := ledArray.io.leds

    // Create a PMOD at base address 0x60
    val pmodA = new GPIO(boardCfg.gpioConfig)
    val pmodABridge = pmodA.driveFrom(peripheralBusCtrl, 0x60)

    // Connect the gpio register to pmodA
    io.pmodA.write <> pmodA.io.dataOut
    pmodA.io.dataIn <> io.pmodA.read
    io.pmodA.writeEnable <> pmodA.io.directions

    // Create two timer and map it at 0xC0 and 0xD0
    val timerA = new Timer(j1Cfg.timerConfig)
    val timerABridge = timerA.driveFrom(peripheralBusCtrl, 0xC0)
    val timerB = new Timer(j1Cfg.timerConfig)
    val timerBBridge = timerB.driveFrom(peripheralBusCtrl, 0xD0)

    // Create an UART interface with fixed capabilities
    val uartCtrlGenerics = UartCtrlGenerics(dataWidthMax = boardCfg.uartConfig.dataWidthMax,
      clockDividerWidth = boardCfg.uartConfig.clockDividerWidth,
      preSamplingSize = boardCfg.uartConfig.preSamplingSize,
      samplingSize = boardCfg.uartConfig.samplingSize,
      postSamplingSize = boardCfg.uartConfig.postSamplingSize)
    val uartCtrlInitConfig = UartCtrlInitConfig(baudrate = boardCfg.uartConfig.baudrate,
      dataLength = boardCfg.uartConfig.dataLength,
      parity = boardCfg.uartConfig.parity,
      stop = boardCfg.uartConfig.stop)
    val uartCtrlMemoryMappedConfig = UartCtrlMemoryMappedConfig(uartCtrlConfig = uartCtrlGenerics,
      initConfig = uartCtrlInitConfig,
      busCanWriteClockDividerConfig = false,
      busCanWriteFrameConfig = false,
      txFifoDepth = boardCfg.uartConfig.fifoDepth,
      rxFifoDepth = boardCfg.uartConfig.fifoDepth)
    val uartCtrl = new UartCtrl(uartCtrlGenerics)

    // Map the UART to 0x80 and enable the generation of read interrupts
    val uartBridge = uartCtrl.driveFrom(peripheralBusCtrl, uartCtrlMemoryMappedConfig, baseAddress = 0x80)
    uartBridge.interruptCtrl.readIntEnable := True

    // Tell Spinal that some unneeded signals are allowed to be pruned to avoid warnings
    uartBridge.interruptCtrl.interrupt.allowPruning()
    uartBridge.write.streamUnbuffered.ready.allowPruning()

    // Create an interrupt controller, map it to 0xE0 and connect all interrupts
    val intCtrl = new InterruptCtrl(j1Cfg)
    val intCtrlBridge = intCtrl.driveFrom(peripheralBusCtrl, 0xE0)
    intCtrl.io.intsE(intCtrl.io.intsE.high downto j1Cfg.irqConfig.numOfInternalInterrupts) <> io.extInt
    intCtrl.io.intsE(0) <> uartBridge.interruptCtrl.readInt
    intCtrl.io.intsE(1) <> timerA.io.interrupt
    intCtrl.io.intsE(2) <> timerB.io.interrupt
    cpu.io.intNo <> intCtrl.io.intNo
    cpu.io.irq <> intCtrl.io.irq

    // Connect the physical UART pins to the outside world
    io.tx := uartCtrl.io.uart.txd
    uartCtrl.io.uart.rxd := io.rx

  }

}

object J1SoC {

  // Make the reset synchron and use the rising edge
  val globalClockConfig = ClockDomainConfig(clockEdge        = RISING,
                                            resetKind        = SYNC,
                                            resetActiveLevel = HIGH)

  def main(args : Array[String]) {

    // Configuration of CPU-core and GPIO system
    val j1Cfg = J1Config.forth
    val boardCfg = BoardConfig.forth

    // Generate all VHDL files
    SpinalConfig(genVhdlPkg = true,
                 defaultConfigForClockDomains = globalClockConfig,
                 defaultClockDomainFrequency = FixedFrequency(80 MHz),
                 targetDirectory="gen/src/vhdl").generateVhdl({

                   // A new system instance
                   new J1SoC(j1Cfg, boardCfg)

                 }).printPruned()

    // Generate all Verilog files
    SpinalConfig(defaultConfigForClockDomains = globalClockConfig,
                 defaultClockDomainFrequency = FixedFrequency(80 MHz),
                 targetDirectory="gen/src/verilog").generateVerilog({

                   // A new system instance
                   new J1SoC(j1Cfg, boardCfg)

                 }).printPruned()

  }

}
