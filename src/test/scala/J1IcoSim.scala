import spinal.sim._
import spinal.core._
import spinal.core.sim._

import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}

import java.awt.Graphics
import javax.swing.{JFrame, JPanel}

object J1IcoSim {

  def main(args: Array[String]): Unit = {

    // Configuration of CPU-core
    val j1Cfg = J1Config.forth

    // Configuration of the used board
    val boardCfg = CoreConfig.icoBoard

    SimConfig(rtl = new J1Ico(j1Cfg, boardCfg)).allOptimisation.doManagedSim{dut =>

      val mainClkPeriod = (1e12 / boardCfg.coreFrequency.getValue.toDouble).toLong
      val uartBaudPeriod = (1e12 / boardCfg.uartConfig.baudrate).toLong

      // Create the global system - clock
      val genClock = fork {

        // Pretend that the clock is already locked
        dut.io.boardClkLocked #= true

        // Make the reset active
        dut.io.reset #= true

        // Release the reset which clock is low
        dut.io.boardClk #= false
        sleep(mainClkPeriod)
        dut.io.reset #= false
        sleep(mainClkPeriod)

        // Init the cycle counter
        var cycleCounter = 0l

        // Get the actual system time
        var lastTime = System.nanoTime()

        // Simulate forever
        while(true){

          // Simulate a clock signal
          dut.io.boardClk #= false
          sleep(mainClkPeriod / 2)
          dut.io.boardClk #= true
          sleep(mainClkPeriod / 2)

          // Advance the simulated cycles value by one
          cycleCounter += 1

          // Check if we should write some information
          if(cycleCounter == 100000){

            // Get current system time
            val currentTime = System.nanoTime()

            // Write some information about simulation speed
            println(f"${cycleCounter / ((currentTime - lastTime) * 1e-9) * 1e-3}%4.0f kHz")

            // Store that current system time and reset the cycle counter
            lastTime = currentTime
            cycleCounter = 0

          }

        }

      }

      // Create an UART receiver
      val uartTx = UartDecoder(uartPin = dut.io.tx,
                               baudPeriod = uartBaudPeriod)

      // Create an UART transiver
      val uartRx = UartEncoder(uartPin = dut.io.rx,
                               baudPeriod = uartBaudPeriod)

      // Start the simulation
      genClock.join()

    }

  }

}
