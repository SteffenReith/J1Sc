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
import spinal.lib.com.uart._

class J1SoC (j1Cfg   : J1Config,
             gpioCfg : GPIOConfig) extends Component {

  val io = new Bundle {

    val leds = out Bits(gpioCfg.ledBankConfig.width bits) // The physical pins for the connected FPGAs
    val rx   =  in Bool // UART input
    val tx   = out Bool // UART output

  }.setName("")

  // Create a new CPU core
  val cpu = new J1(j1Cfg)

  // Create a delayed version of the cpu core interface to GPIO
  val peripheralBus = cpu.io.cpuBus.delayed(gpioCfg.gpioWaitStates)
  val peripheralBusCtrl = SimpleBusSlaveFactory(peripheralBus)

  // Create a LED bank at base address 0x100
  val ledBank = new LEDBank(gpioCfg.ledBankConfig)
  val ledBridge = ledBank.driveFrom(peripheralBusCtrl, 0x100)

  // Create an UART interface
  val uartCtrlGenerics = UartCtrlGenerics(dataWidthMax      = 8,
                                          clockDividerWidth = 20,
                                          preSamplingSize   = 1,
                                          samplingSize      = 5,
                                          postSamplingSize  = 2)
  val uartCtrlInitConfig = UartCtrlInitConfig(baudrate = 115200,
                                              dataLength = 8,
                                              parity = UartParityType.NONE,
                                              stop = UartStopType.ONE)
  val uartCtrlMemoryMappedConfig = UartCtrlMemoryMappedConfig(uartCtrlConfig = uartCtrlGenerics,
                                                              initConfig = uartCtrlInitConfig,
                                                              busCanWriteClockDividerConfig = false,
                                                              busCanWriteFrameConfig = false,
                                                              txFifoDepth = 8,
                                                              rxFifoDepth = 8)
  val uartCtrl = new UartCtrl(uartCtrlGenerics)
  val uartBridge = uartCtrl.driveFrom(peripheralBusCtrl, uartCtrlMemoryMappedConfig)

  // Connect the physical UART pins to the outside world
  io.tx := uartCtrl.io.uart.txd
  uartCtrl.io.uart.rxd := io.rx

  // Connect the physical LED pins to the outside world
  io.leds := ledBank.io.leds

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
