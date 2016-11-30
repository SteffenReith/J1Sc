/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Tue Nov 1 14:34:09 GMT+1 2016
 * Module Name:    J1SoC - A small but complete system based on the J1-core
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: ec2c5c04988bb976040975bae0e46966d21cc9e9
 * Date: Tue Nov 1 15:34:51 2016 +0100
 */
import spinal.core._
import spinal.lib._
import spinal.lib.com.uart._

class J1SoC (j1Cfg   : J1Config,
             gpioCfg : GPIOConfig) extends Component {

  val io = new Bundle {

    // Asynchronous interrupts for the outside world
    val extInt = in Bits(j1Cfg.noOfInterrupts - j1Cfg.noOfInternalInterrupts bits)

    // The physical pins for the connected FPGAs
    val leds = out Bits(gpioCfg.ledBankConfig.width bits)

    // I/O pins for the UART
    val rx   =  in Bool // UART input
    val tx   = out Bool // UART output

  }.setName("")

  // Create a new CPU core
  val cpu = new J1(j1Cfg)

  // Create a delayed version of the cpu core interface to GPIO
  val peripheralBus = cpu.io.cpuBus.delayed(gpioCfg.gpioWaitStates)
  val peripheralBusCtrl = SimpleBusSlaveFactory(peripheralBus)

  // Create a LED bank at base address 0x40
  val ledBank = new LEDBank(gpioCfg.ledBankConfig)
  val ledBridge = ledBank.driveFrom(peripheralBusCtrl, 0x40)

  // Connect the physical LED pins to the outside world
  io.leds := ledBank.io.leds

  // Create an UART interface
  val uartCtrlGenerics = UartCtrlGenerics(dataWidthMax      = gpioCfg.uartConfig.dataWidthMax,
                                          clockDividerWidth = gpioCfg.uartConfig.clockDividerWidth,
                                          preSamplingSize   = gpioCfg.uartConfig.preSamplingSize,
                                          samplingSize      = gpioCfg.uartConfig.samplingSize,
                                          postSamplingSize  = gpioCfg.uartConfig.postSamplingSize)
  val uartCtrlInitConfig = UartCtrlInitConfig(baudrate = gpioCfg.uartConfig.baudrate,
                                              dataLength = gpioCfg.uartConfig.dataLength,
                                              parity = gpioCfg.uartConfig.parity,
                                              stop = gpioCfg.uartConfig.stop)
  val uartCtrlMemoryMappedConfig = UartCtrlMemoryMappedConfig(uartCtrlConfig = uartCtrlGenerics,
                                                              initConfig = uartCtrlInitConfig,
                                                              busCanWriteClockDividerConfig = false,
                                                              busCanWriteFrameConfig = false,
                                                              txFifoDepth = gpioCfg.uartConfig.fifoDepth,
                                                              rxFifoDepth = gpioCfg.uartConfig.fifoDepth)
  val uartCtrl = new UartCtrl(uartCtrlGenerics)

  // Map the UART to 0x80
  val uartBridge = uartCtrl.driveFrom16(peripheralBusCtrl, uartCtrlMemoryMappedConfig, baseAddress = 0x80)

  // Connect the physical UART pins to the outside world
  io.tx := uartCtrl.io.uart.txd
  uartCtrl.io.uart.rxd := io.rx

  // Create an interrupt controller and connect it
  val intCtrl = new InterruptCtrl(noOfInterrupts = j1Cfg.noOfInterrupts)
  intCtrl.io.intsE(intCtrl.io.intsE.high downto j1Cfg.noOfInternalInterrupts) <> io.extInt
  intCtrl.io.intsE(0) := uartBridge.interruptCtrl.readInt
  cpu.io.intNo <> intCtrl.io.intNo
  cpu.io.irq <> intCtrl.io.irq

}

object J1SoC {

  // Make the reset synchron and use the rising edge
  val globalClockConfig = ClockDomainConfig(clockEdge        = RISING,
                                            resetKind        = SYNC,
                                            resetActiveLevel = HIGH)

  def main(args : Array[String]) {

    // Configuration of CPU and GPIO system
    val j1Cfg = J1Config.debug
    val gpioCfg = GPIOConfig.default

    // Generate HDL files
    SpinalConfig(genVhdlPkg = true,
                 defaultConfigForClockDomains = globalClockConfig,
                 defaultClockDomainFrequency = FixedFrequency(100 MHz),
                 targetDirectory="gen/src/vhdl").generateVhdl({

                   // Set name for the synchronous reset
                   ClockDomain.current.reset.setName("clr")
                   new J1SoC(j1Cfg, gpioCfg)

                 }).printPruned()
    SpinalConfig(defaultConfigForClockDomains = globalClockConfig,
                 defaultClockDomainFrequency = FixedFrequency(100 MHz),
                 targetDirectory="gen/src/verilog").generateVerilog({

                   // Set name for the synchronous reset
                   ClockDomain.current.reset.setName("clr")
                   new J1SoC(j1Cfg, gpioCfg)

                 }).printPruned()

  }

}
