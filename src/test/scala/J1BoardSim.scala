import scala.sys.exit

import spinal.core._
import spinal.core.sim._

import scopt.OptionParser
import jssc._

import java.net.ServerSocket
import java.io.{InputStream, OutputStream}
import java.awt.{BorderLayout, Color, Graphics}
import javax.swing.{JButton, JFrame, JPanel, WindowConstants}
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

// Implement 8N1
object UARTReceiver {

  // Create an receiver which gets data from the simulation
  def apply(output : SerialPort, uartPin : Bool, baudPeriod : Long) = fork {

    // An UART is high inactive -> wait until simulation starts
    waitUntil(uartPin.toBoolean)

    // Give some information about the UART receiver
    println("[J1Sc] Start receiver simulation")

    // Simulate the Receiver forever
    while (true) {

      print("Data to receive : ")

      // Look for the falling start bit and wait a half bit time (middle of start bit)
      waitUntil(!uartPin.toBoolean)
      sleep(baudPeriod / 2)

      // Wait until the middle of the first data bit
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

      println(buffer.toByte + "(received)")

    }

  }

}

object UARTTransceiver {

  // Create an transceiver which sends data to the simulation
  def apply(input : SerialPort, uartPin : Bool, baudPeriod : Long) = fork {

    // Make the line inactive (high)
    uartPin #= true

    // Give some information about the transceiver simulation
    println("[J1Sc] Start transceiver simulation")

    // Simulate the data transmission forever
    while(true) {

      // Check if there is data send by the host
      if(input.getInputBufferBytesCount > 0){

        // get one byte from the host (remember the return type of 'readBytes' is an array of bytes)
        val buffer = input.readBytes(1)(0)

        println("To transmit : " + buffer)

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

    // Configuration for simulation, CPU-core and simulated evaluation board
    val simCfg = J1SimConfig.default
    val j1Cfg = J1Config.forth16Jtag
    val boardCfg = CoreConfig.boardSim

    // Holds the compile name of the used serial device
    var serialDeviceName : String = null

    // Holds the flag to indicate whether a wave-file should be generated
    var createWaveFile : Boolean = false

    // Create a new scopt parser
    val parser = new OptionParser[ArgsConfig]("J1ScSim") {

      // A simple header for the help text
      head("J1ScSim - A gate-level simulation of J1Sc", "")

      // Option to enable the /dev - Prefix
      opt[Unit]("useDevicePrefix").action { (_, c) => c.copy(useDevicePrefix = true) }.
                                   text("Use a device prefix (default: not used)")

      // Option to enable wave-file creation
      opt[Unit]("createWaveFile").action { (_, c) => c.copy(createWaveFile = true) }.
                                  text("Create a wave-file. WARNING: Can be huge! (default: not active)")

      // Option for setting the name of the serial device
      opt[String]("serialDeviceName").action {(s,c) => c.copy(serialDeviceName = s) }.
                                      text("Set name of serial device (default: " + simCfg.serialDeviceName + ")")

      // Option for setting the name of the serial device
      opt[String]("serialDevicePrefix").action { (s, c) => c.copy(serialDevicePrefix = s) }.
                                        text("Set the serial device prefix (default: " + simCfg.devicePrefix + ")")

      // Help option
      help("help").text("print this text")

    }
    parser.parse(args, ArgsConfig(useDevicePrefix    = false,
                                  createWaveFile     = false,
                                  serialDeviceName   = simCfg.serialDeviceName,
                                  serialDevicePrefix = simCfg.devicePrefix)).map { cfg =>

      // Check if we use the device prefix and build the device-name
      serialDeviceName = (if (cfg.useDevicePrefix) cfg.serialDevicePrefix + "/" else "") + cfg.serialDeviceName

      // Check if a waveFile should be generated
      createWaveFile = cfg.createWaveFile

    } getOrElse {

      // Terminate program with error-code (wrong argument / option)
      exit(1)

    }

    // Time resolution of the simulation is 1ns
    val simTimeRes = simCfg.simTimeRes

    // Number of CPU cycles between some status information
    val simCycles = simCfg.simCycles

    // Flag for doing a reset (set to true for initial reset)
    var doReset = true

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

    // Create a simulation (first check if we want to have a simulation result)
    val simJ1 = if (createWaveFile) SimConfig.withWave else SimConfig
    simJ1.workspacePath("gen/sim")
         .allOptimisation
         .compile(new J1Ico(j1Cfg, boardCfg)).doSimUntilVoid { dut =>

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

      // Receive data from the host OS and send it into the simulation
      UARTTransceiver(input = comPort, uartPin = dut.io.rx, baudPeriod = uartBaudPeriod)

      // Transmit data from the simulation into the host OS
      UARTReceiver(output = comPort, uartPin = dut.io.tx, baudPeriod = uartBaudPeriod)

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
        var cycleCounter = 0L

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
      fork {

        // Simulate forever
        while(true) {

          // Wait until we should do a reset
          waitUntil(doReset)

          // Give a short message about the reset
          println("[J1Sc] Asynchronous reset CPU")

          // Do a reset cycle for some cycles
          DoReset(dut.io.reset, 100, HIGH)

          // Wait for next reset event
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
        fork {

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
                println("[J1Sc] New TCP connection for JTAG simulation")

              }

            }

          }
          server.start()

          // Forever handle new data
          while (true) {

            // Wait for some JTAG clock cycles
            sleep(jtagClkPeriod * 200)

            // Check if new input data is available
            while ((inStream != null) && (inStream.available() != 0)) {

              // Get the new data
              val buffer = inStream.read()

              // Decode virtual JTAG data and write is to the dut
              dut.jtagCondIOArea.jtag.tms #= (buffer & 1) != 0
              dut.jtagCondIOArea.jtag.tdi #= (buffer & 2) != 0
              dut.jtagCondIOArea.jtag.tck #= (buffer & 8) != 0

              // Check if we have to read back some data
              if ((buffer & 4) != 0) {

                // Send back the data given by the jtag interface
                outStream.write(if (dut.jtagCondIOArea.jtag.tdo.toBoolean) 1 else 0)

              }

              // Wait half a JTAG cycle
              sleep(jtagClkPeriod / 2)

            }

          }

        }

      }

      // Simulate the leds array
      fork {

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
          setSize(320, 120)
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
            val ledOnColor  = Color.green.brighter()
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
      
      // Terminate all threads and the simulation
      genClock.join()

      // Close the serial port (never reached)
      //assert(comPort.closePort())

    }

  }

}
