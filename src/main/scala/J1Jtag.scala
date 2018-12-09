/*
 * Author: Steffen Reith (steffen.reith@hs-rm.de)
 *
 * Creation Date:  Sat Sep 22 21:50:27 GMT+2 2018
 * Module Name:    JTAG - A JTAG implementation for the J1 processor
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

class J1Jtag(j1Cfg   : J1Config,
             jtagCfg : JTAGConfig) extends Component {

  // Signals used for the JTAG interface
  val io = new Bundle {

    // JTAG data input
    val tdi = in Bool

    // JTAG data output
    val tdo = out Bool

    // Control for the JTAG TAP
    val tms = in Bool

    // The JTAG clock (the signal tdi, tdo and tms are synchron to this clock)
    val tck = in Bool

  }.setName("")

  // All internal signals
  val internal = new Bundle {

    // Indicate that the CPU has to halted
    val halt = out Bool

  }.setName("")

  // Create a register of width bits if condition createIt is true
  def ConditionalCreateRegister(createIt : Boolean, width : BitCount)= {

    // Check the condition
    if (createIt) Reg(Bits(width)) else null

  }

  // Create a clockdomain which is synchron to tck but used a global synchron reset
  val jtagClockDomain = ClockDomain(clock = io.tck, reset = ClockDomain.current.reset)

  // Create the clock area used for the JTAG
  val jtagArea = new ClockingArea(jtagClockDomain) {

    // Patterns to check whether a JTAG command has a read, write or output constant semantic
    val readModePattern     = ".*r.*"
    val writeModePattern    = ".*w.*"
    val constantModePattern = ".*c.*"

    // JTAG-command to be implemented (format Name x ID x Width x Mode)
    val bypassCmd = ("BYPASS", B(jtagCfg.irWidth bits, default -> True), 1,              "rw")
    val idcodeCmd = ("IDCODE", B(1, jtagCfg.irWidth bits),               j1Cfg.wordSize, " c")
    val haltCmd   = ("HALT",   B(2, jtagCfg.irWidth bits),               1,              "rw")

    // List of all implemented JTAG-commands, where BYPASS is mandatory and has to be the first entry
    val jtagCommands = (bypassCmd :: (idcodeCmd :: (haltCmd :: Nil)))
    
    // The JTAG instruction register
    val instructionShiftReg = Reg(Bits(jtagCfg.irWidth bits))
    val instructionReg = Reg(Bits(jtagCfg.irWidth bits))

    // For all JTAG instructions which needs a data register
    val dataRegs = Vec(for((name, _, width, mode) <- jtagCommands) yield {

      // Write a message
      println("[J1Sc]   Create register for JTAG command " +
              name +
              " (Width is " +
              width +
              " bits) with mode >>" +
              mode +
              "<<")

        // Create the corresponding data register
        ConditionalCreateRegister(! mode.matches(constantModePattern), width bits)

    })

    // For all JTAG instructions
    val dataShiftRegs = Vec(for((_, _, width, _) <- jtagCommands) yield {

      // Create the corresponding data register
      Reg(Bits(width bits))

    })


    // Create the JTAG FSM (see https://www.fpga4fun.com/JTAG2.html)
    val jtagFSM = new StateMachine {

      // Reset the FSM and init the registers if needed
      val testLogicReset = new State with EntryPoint

      // Idle the JTAG FSM
      val runTestIdle    = new State

      // States related to the data register
      val selectDRScan   = new State
      val captureDR      = new State
      val shiftDR        = new State
      val exit1DR        = new State
      val pauseDR        = new State
      val exit2DR        = new State
      val updateDR       = new State

      // States related to the instruction register
      val selectIRScan   = new State
      val captureIR      = new State
      val shiftIR        = new State
      val exit1IR        = new State
      val pauseIR        = new State
      val exit2IR        = new State
      val updateIR       = new State

      // Handle idle and reset state of the JTAG FSM
      testLogicReset.whenIsActive {

        // Init the HALT data register
        dataRegs(jtagCommands.indexOf(haltCmd))(0) := False
        dataShiftRegs(jtagCommands.indexOf(haltCmd))(0) := False

        // Init the register for the idcode command
        dataShiftRegs(jtagCommands.indexOf(idcodeCmd)) := B(jtagCfg.idCodeValue)

        // Implement the transition logic
        when(io.tms) {goto(testLogicReset)} otherwise{goto(runTestIdle)}

      }
      runTestIdle.whenIsActive{when(io.tms) {goto(selectDRScan)} otherwise{goto(runTestIdle)}}

      // Define the transition function for states related to the data register
      selectDRScan.whenIsActive{when(io.tms) {goto(selectIRScan)} otherwise{goto(captureDR)}}
      exit1DR.whenIsActive{when(io.tms) {goto(updateDR)} otherwise{goto(pauseDR)}}
      pauseDR.whenIsActive{when(io.tms) {goto(exit2DR)} otherwise{goto(pauseDR)}}
      exit2DR.whenIsActive{when(io.tms) {goto(updateDR)} otherwise{goto(shiftDR)}}

      // Handle the capture of all data registers (copy content of data register to the corresponding shift register)
      captureDR.whenIsActive {

        // Generate decoders for all JTAG data registers
        for (((_, id, _, mode), i) <- jtagCommands.zipWithIndex) {

          // Check whether we need a read mode
          if (mode.matches(readModePattern)) {

            // Generate decoder for the ith data register
            when (instructionReg === id) {

              // Capture the ith data register
              dataShiftRegs(i) := dataRegs(i)

            }

          }

        }

        // Define the transition function for this state
        when(io.tms) {goto(exit1DR)} otherwise{goto(shiftDR)}

      }

      // Handle the data shift-register
      shiftDR.whenIsActive {

        // Generate code of all JTAG data shift registers
        for(((_ ,id ,_ ,_), i) <- jtagCommands.zipWithIndex) {

          // Generate decoder for the ith data register
          when (instructionReg === id) {

            // Add data to ith shift register
            dataShiftRegs(i) := (io.tdi ## dataShiftRegs(i)) >> 1

          }

        }

        // Define the transition function for this state
        when(io.tms) {goto(exit1DR)} otherwise{goto(shiftDR)}

      }

      // Move the received data for the shift register to data register
      updateDR.whenIsActive {

        // Generate decoder for all JTAG data registers
        for(((_,id ,_ ,mode), i) <- jtagCommands.zipWithIndex) {

          // Check whether we need a write mode access
          if(mode.matches(writeModePattern)) {

            // Generate decoder for the ith data register
            when (instructionReg === id) {

              // Update the ith data register
              dataRegs(i) := dataShiftRegs(i)

            }

          }

        }

        // Define the transition function for the this state
        when(io.tms) {goto(selectDRScan)} otherwise{goto(runTestIdle)}

      }

      // Define the transisition function for states related to the instruction register
      selectIRScan.whenIsActive{when(io.tms) {goto(testLogicReset)} otherwise{goto(captureIR)}}
      exit1IR.whenIsActive{when(io.tms) {goto(updateIR)} otherwise{goto(pauseIR)}}
      pauseIR.whenIsActive{when(io.tms) {goto(exit2IR)} otherwise{goto(pauseIR)}}
      exit2IR.whenIsActive{when(io.tms) {goto(updateIR)} otherwise{goto(shiftIR)}}

      // Handle the capture of the instruction register
      captureIR.whenIsActive {

        // Capture the current instruction into the instruction shift register
        instructionShiftReg := instructionReg

        // Define the transition function for this state
        when(io.tms) {goto(exit1IR)} otherwise{goto(shiftIR)}

      }

      // Handle the shift-register
      shiftIR.whenIsActive {

        // Feed lsb of instruction register to tdo
        io.tdo := instructionShiftReg.lsb

        // Add data to shift register
        instructionShiftReg := (io.tdi ## instructionShiftReg) >> 1

        // Define the transition function for the this state
        when(io.tms) {goto(exit1IR)} otherwise{goto(shiftIR)}

      }

      // Move the next instruction (received in the shift register) to the instruction register
      updateIR.whenIsActive {

        // Copy data from shift register to the instruction register
        instructionReg := instructionShiftReg

        // Define the transition function for the this state
        when(io.tms) {goto(selectDRScan)} otherwise{goto(runTestIdle)}

      }

    }

    // Avoid a latch and feed the lsb of BYPASS to tdo
    io.tdo := dataShiftRegs(0).lsb

    // Generate code of all JTAG data shift registers
    for(((_ ,id ,_ ,_), i) <- jtagCommands.zipWithIndex) {

      // check whether the command with id is a active
      when (instructionReg === id) {

        // Add data to ith shift register
        io.tdo := dataShiftRegs(i).lsb

      }

    }

  }

  // Find the data register of HALT, do a clock domain crossing and provide this information externally
  internal.halt := BufferCC(jtagArea.dataRegs(jtagArea.jtagCommands.indexOf(jtagArea.haltCmd))(0))

}
