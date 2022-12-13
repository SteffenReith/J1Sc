import scala.sys.exit

import spinal.core._
import spinal.core.sim._

import jssc._

import java.net.ServerSocket
import java.io.{InputStream, OutputStream}

import java.awt.{BorderLayout, Color, Graphics}

import javax.swing.{JButton, JFrame, JPanel, WindowConstants}
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

object UARTReceiver {

  // Create an receiver which gets data from the simulation
  def apply(output : SerialPort, uartPin : Bool, baudPeriod : Long) = fork {

    // An UART is high inactive -> wait until simulation starts
    waitUntil(uartPin.toBoolean)

    // Give some information about the UART receiver
    println("[J1Sc] Start Receiver simulation")

    // Simulate the Receiver forever
    while (true) {

      // Look for the rising start bit and wait a half bit time
      waitUntil(!uartPin.toBoolean)
      sleep(baudPeriod / 2)

      // Check if start bit is still active and wait for first data bit
      sleep(baudPeriod)

      // Hold the received byte
      var buffer = 0

      // Read all 8 data bits
      (0 to 7).foreach { bitIdx =>

        // Check the actual data bit
        if (uartPin.toBoolean) {

          // Add a 1 to the received byte
          buffer |= 1 << bitIdx

        }

        // Wait for the next data bit
        sleep(baudPeriod)

      }

      // Write character
      output.writeByte(buffer.toByte)

    }

  }

}

object UARTTransceiver {

  // Create an transceiver which sends data to the simulation
  def apply(input : SerialPort, uartPin : Bool, baudPeriod : Long) = fork {

    // Make the line inactive (high)
    uartPin #= true

    // Give some information about the transiver simulation
    println("[J1Sc] Start transceiver simulation")

    // Simulate the data transmission forever
    while(true) {

      // Check if there is data send by the host
      if(input.getInputBufferBytesCount > 0){

        // get one byte from the host (remember the return type of 'readBytes' is an array of bytes)
        val buffer = input.readBytes(1)(0)

        // Create the start bit
        uartPin #= false
        sleep(baudPeriod)

        // Send 8 data bits
        (0 to 7).foreach{ bitIdx =>

          // Send bit at position "bitIdx"
          uartPin #= ((buffer >> bitIdx) & 1) != 0
          sleep(baudPeriod)

        }

        // Create stop bit
        uartPin #= true
        sleep(baudPeriod)

      } else {

        // Sleep 10 bit times since no data is available
        sleep(baudPeriod * 10)

      }

    }

  }

}

object J1BoardSim {

  def main(args: Array[String]) : Unit = {

    // Time resolution of the simulation is 1ns
    val simTimeRes = 1e9

    // Number of CPU cycles between some status information
    val simCycles = 10000000l

    // Name of serial device to be used
    val serialDeviceName = "tnt1"

    // Configuration of CPU-core
    val j1Cfg = J1Config.forth16Jtag

    // Configuration of the used board
    val boardCfg = CoreConfig.boardSim
	
    // Flag for doing a reset
    var doReset = false

    // Open the (pseudo) serial connection
    println("[J1Sc]  Try to open the serial device " + serialDeviceName)
    val comPort = new SerialPort(serialDeviceName)

    try {

      comPort.openPort()
      comPort.setParams(SerialPort.BAUDRATE_38400,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE)

    } catch {

      case e : Exception =>

        // Unknown exception
        println("[J1Sc] Cannot open " + serialDeviceName)
        println("[J1Sc] Have to terminate simulation!")

        // Terminate program
        exit(-1)

    }

    SimConfig.workspacePath("gen/sim")
             .allOptimisation
             //.withWave
             .compile(new J1Ice(j1Cfg, boardCfg)).doSimUntilVoid { dut =>

      // Calculate the number of verilog ticks relative to the given time resolution
      val mainClkPeriod  = (simTimeRes / boardCfg.coreFrequency.getValue.toDouble).toLong
      val uartBaudPeriod = (simTimeRes / boardCfg.uartConfig.baudRate.toDouble).toLong

      // Print some information about the simulation
      println("[J1Sc] Start the simulation of a J1Sc on an IcoBoard")
      println("[J1Sc]  Board frequency is " + boardCfg.coreFrequency.getValue.toDouble + " Hz")
      println("[J1Sc]  UART transmission rate " + boardCfg.uartConfig.baudRate.toLong + " bits/sec")
      println("[J1Sc]  Time resolution is " + 1.0 / simTimeRes + " sec")
      println("[J1Sc]  One clock period in ticks is " + mainClkPeriod  + " ticks")
      println("[J1Sc]  Bit time (UART) in ticks is "  + uartBaudPeriod + " ticks")

      // Init the system and create the global system clock
      val genClock = fork {

        // Pretend that the clock is already locked and low
        dut.io.boardClkLocked #= true
        dut.io.boardClk       #= false

        // Create the clock signal using the threadless API
        ForkClock(dut.io.boardClk, mainClkPeriod)

        // Force (note that reset can be 1 after startup) a reset cycle for some cycles
        dut.io.reset #= false
        sleep(cycles = 1)
        DoReset(dut.io.reset, 100, HIGH)

        // Init the cycle counter
        var cycleCounter = 0l

        // Get the actual system time to init the time calculation
        var lastTime = System.nanoTime()

        // The reset is simulated using the threaded API
        ClockDomain(dut.io.boardClk).onSamplings {

          // Advance the simulated cycles value by one
          cycleCounter += 1

          // Check if we should write some information
          if (cycleCounter == simCycles) {

            // Get current system time
            val currentTime = System.nanoTime()

            // Write information about simulation speed
            val deltaTime = (currentTime - lastTime) * 1e-9
            val speedUp = (simCycles.toDouble /
                           boardCfg.coreFrequency.getValue.toDouble) / deltaTime
            println(f"[J1Sc] $simCycles cycles in $deltaTime%4.2f real seconds (Speedup: $speedUp%4.3f)")

            // Store the current system time for the next round and reset the cycle counter
            lastTime = currentTime
            cycleCounter = 0

          }

        }

      }

      // Handle a reset request using the threaded API
      val resetHandler = fork {

        // Simulate forever
        while(true) {

          // Wait until we should do a reset
          waitUntil(doReset)

          // Give a short message about the reset
          println("[J1Sc] Asynchronous reset CPU")

          // Do a reset cycle for some cycles
          DoReset(dut.io.reset, 100, HIGH)

          // Clear the reset flag
          doReset = false

        }

      }

      // Check if we have an jtag interface
      j1Cfg.hasJtag generate {

        // Calculate the number of ticks of a jtag clock cycle
        val jtagClkPeriod = (simTimeRes / j1Cfg.jtagConfig.jtagFreq.getValue.toDouble).toLong

        // Give some information about the number of ticks of a jtag clock cycle
        println("[J1Sc]  Jtag Cycle time in ticks is " + jtagClkPeriod + " ticks")

        // Build a simulation for the JTAG interface
        val jtag = fork {

          // Input an output data for server socket
          var inStream  : InputStream  = null
          var outStream : OutputStream = null

          // Create the server thread
          val server = new Thread {

            // Specify with code has to run in this thread
            override def run() : Unit = {

              // Start the socket
              val socket = new ServerSocket(7894)

              // Give some information
              println("[J1Sc] Waiting for tcp connection on port 7894")

              // Run the server forever
              while (true) {

                // Accept data
                val connection = socket.accept()
                connection.setTcpNoDelay(true)

                // Connect the data streams to the socket
                outStream = connection.getOutputStream
                inStream  = connection.getInputStream

                // Report that we handled data
                println("[J1Sc] New TCP connection for jtag simulation")

              }

            }

          }
          server.start()

          // Forever handle new data
          while (true) {

            // Wait for some jtag clock cycles
            sleep(jtagClkPeriod * 200)

            // Check if new input data is available
            while ((inStream != null) && (inStream.available() != 0)) {

              // Get the new data
              val buffer = inStream.read()

              // Decode virtual jtag data and write is to the dut
              dut.jtagCondIOArea.jtag.tms #= (buffer & 1) != 0
              dut.jtagCondIOArea.jtag.tdi #= (buffer & 2) != 0
              dut.jtagCondIOArea.jtag.tck #= (buffer & 8) != 0

              // Check if we have to read back some data
              if ((buffer & 4) != 0) {

                // Send back the data given by the jtag interface
                outStream.write(if (dut.jtagCondIOArea.jtag.tdo.toBoolean) 1 else 0)

              }

              // Wait half a jtag cycle
              sleep(jtagClkPeriod / 2)

            }

          }

        }

      }

      // Simulate the leds array
      val leds = fork {

        // Holds the value represented by the leds array
        var ledsValue = 0l

        // Create a graphical frame using Java
        val ledsFrame = new JFrame("J1Sc Components") {

          // Dimensions of the simulated leds
          val ledBorderWidth = 5
          val ledDiameter = 20

          // Create a new contents panel
          val mainPanel = new JPanel(new BorderLayout())
          setContentPane(mainPanel)

          // Create the led panel
          val ledPanel = new LEDPanel()
          ledPanel.setSize(boardCfg.ledBankConfig.width * ledDiameter, ledDiameter + ledBorderWidth)

          // Add the led panel to the main panel
          mainPanel.add(ledPanel)

          // Create a panel and a reset push button
          val resetPanel = new JPanel()
          val resetButton = new JButton("Reset CPU")
          resetButton.addActionListener(new ResetButtonHandler())
          resetPanel.add(resetButton)

          // Add the button panel to the main panel
          mainPanel.add(resetPanel, BorderLayout.SOUTH)

          // Terminate program when close button is used
          setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

          // Set a useful frame size and make is visible
          setSize(300, 100)
          setVisible(true)

          // Create an event handler for checking the reset button
          class ResetButtonHandler extends ActionListener {

            override def actionPerformed(event : ActionEvent) : Unit = {

              // Check if the event source was the reset button
              if (event.getSource == resetButton) {

                // Set the reset flag
                doReset = true

              }

            }

          }

          //Create a component that you can actually draw on
          class LEDPanel extends JPanel {

            // Create the colors for a led in either on or off state
            val ledOnColor = Color.green.brighter()
            val ledOffColor = Color.green.darker()

            // Implement the paint method for repainting the component
            override def paintComponent(g : Graphics) : Unit = {

              // Set the color for the outer ring
              g.setColor(Color.BLACK)

              // Draw some ovals as a decoration
              for(i <- 0 until boardCfg.ledBankConfig.width) {

                // Fill the the ith area
                g.fillOval(ledDiameter * i, ledDiameter, ledDiameter, ledDiameter)


              }

              // Now draw all leds of the led array
              for(i <- 0 until boardCfg.ledBankConfig.width) {

                // Check for the ith led
                if (((ledsValue >> (boardCfg.ledBankConfig.width - (i + 1))) & 1) != 0) {

                  // Set the color to led on
                  g.setColor(ledOnColor)


                } else {

                  // Set the color to led off
                  g.setColor(ledOffColor)

                }

                // Fill the the ith led
                g.fillOval(ledDiameter * i + ledBorderWidth, ledDiameter + ledBorderWidth,
                           ledDiameter - 2 * ledBorderWidth, ledDiameter - 2 * ledBorderWidth)

              }

            }

          }

        }

        // Simulate forever
        while(true) {

          // Wait for 100000 CPU cycles
          sleep(mainClkPeriod * 100000)

          // Get the new leds value and repaint it
          ledsValue = dut.io.leds.toLong
          ledsFrame.repaint()

        }

      }

      // Transmit data from the simulation into the host OS
      UARTReceiver(output = comPort, uartPin = dut.io.tx, baudPeriod = uartBaudPeriod)

      // Receive data from the host OS and send it into the simulation
      UARTTransceiver(input = comPort, uartPin = dut.io.rx, baudPeriod = uartBaudPeriod)

      // Terminate all threads and the simulation
      genClock.join()

      // Close the serial port (never reached)
      //assert(comPort.closePort())

    }

  }

}
