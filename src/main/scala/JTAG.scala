/*
 * Author: Steffen Reith (steffen.reith@hs-rm.de)
 *
 * Creation Date:  Sat Sep 22 21:50:27 GMT+2 2018
 * Module Name:    JTAG - A JTAG implementation for the J1 processor
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._
import spinal.lib.fsm._

class JTAG(j1Cfg   : J1Config,
           jtagCfg : JTAGConfig) extends Component {

  // Signals used for the JTAG interface
  val jtagIO = new Bundle {

    // JTAG data input
    val tdi = in Bool

    // JTAG data output
    val tdo = out Bool

    // Control for the JTAG TAP
    val tms = in Bool

    // The JTAG clock (the signal tdi, tdo and tms are synchron to this clock)
    val tck = in Bool

  }.setName("")

  // Create a clockdomain which is synchron to tck but used a global synchron reset
  val jtagClockDomain = ClockDomain(clock = jtagIO.tck, reset = ClockDomain.current.reset)

  // Create the clock area used for the JTAG
  val jtagArea = new ClockingArea(jtagClockDomain) {

    // List of all implemented JTAG-commands (format Name x ID x Width)
    val jtagCommands = ("BYPASS", B(jtagCfg.irWidth bits, default -> True), 1) :: (
                       ("IDCODE", B(1, jtagCfg.irWidth bits), j1Cfg.wordSize) :: Nil)

    // The JTAG instruction register
    val instructionShiftReg = Reg(Bits(jtagCfg.irWidth bits))
    val instructionReg = Reg(Bits(jtagCfg.irWidth bits))

    // For all JTAG instructions
    val dataRegs = Vec(for((name,_,width) <- jtagCommands) yield {

      // Write a message
      println("[J1Sc]   Create register for JTAG command " +
              name +
              " (Width is " +
              width +
              " bits)")

      // Create the corresponding data register
      Reg(Bits(width bits))

    })

    // For all JTAG instructions
    val dataShiftRegs = Vec(for((_,_,width) <- jtagCommands) yield {

      // Create the corresponding data register
      Reg(Bits(width bits))

    })

    // Create the JTAG FSM (see https://www.fpga4fun.com/JTAG2.html)
    val jtagFSM = new StateMachine {

      // Idle and reset of the JTAG FSM
      val testLogicReset = new State with EntryPoint
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

      // Handle idle and reset state of the JTAG fsm
      testLogicReset.whenIsActive{when(jtagIO.tms) {goto(testLogicReset)} otherwise{goto(runTestIdle)}}
      runTestIdle.whenIsActive{when(jtagIO.tms) {goto(selectDRScan)} otherwise{goto(runTestIdle)}}

      // Define the transisition function for states related to the data register
      selectDRScan.whenIsActive{when(jtagIO.tms) {goto(selectIRScan)} otherwise{goto(captureDR)}}
      captureDR.whenIsActive{when(jtagIO.tms) {goto(exit1DR)} otherwise{goto(shiftDR)}}
      exit1DR.whenIsActive{when(jtagIO.tms) {goto(updateDR)} otherwise{goto(pauseDR)}}
      pauseDR.whenIsActive{when(jtagIO.tms) {goto(exit2DR)} otherwise{goto(pauseDR)}}
      exit2DR.whenIsActive{when(jtagIO.tms) {goto(updateDR)} otherwise{goto(shiftDR)}}
      updateDR.whenIsActive{when(jtagIO.tms) {goto(selectDRScan)} otherwise{goto(runTestIdle)}}

      // Handle the data shift-register
      shiftDR.whenIsActive {

        // Add data to shift register
        //dataReg := instructionReg(dataReg.high - 1 downto 0) ## jtagIO.tdi

        // Define the transition function for this state
        when(jtagIO.tms) {goto(exit1DR)} otherwise{goto(shiftDR)}

      }

      // Define the transisition function for states related to the instruction register
      selectIRScan.whenIsActive{when(jtagIO.tms) {goto(testLogicReset)} otherwise{goto(captureIR)}}
      exit1IR.whenIsActive{when(jtagIO.tms) {goto(updateIR)} otherwise{goto(pauseIR)}}
      pauseIR.whenIsActive{when(jtagIO.tms) {goto(exit2IR)} otherwise{goto(pauseIR)}}
      exit2IR.whenIsActive{when(jtagIO.tms) {goto(updateIR)} otherwise{goto(shiftIR)}}

      // Handle the capture of the instruction register
      captureIR.whenIsActive {

        // Capture the current instruction into the instruction shift register
        instructionShiftReg := instructionReg

        // Define the transition function for this state
        when(jtagIO.tms) {goto(exit1IR)} otherwise{goto(shiftIR)}

      }

      // Handle the shift-register
      shiftIR.whenIsActive {

        // Add data to shift register
        instructionShiftReg := (jtagIO.tdi ## instructionReg) >> 1

        // Define the transition function for the this state
        when(jtagIO.tms) {goto(exit1IR)} otherwise{goto(shiftIR)}

      }

      // Move the next instruction (received in the shift register) to the instruction register
      updateIR.whenIsActive {

        // Copy data from shift register to the instruction register
        instructionReg := instructionShiftReg

        // Define the transition function for the this state
        when(jtagIO.tms) {goto(selectDRScan)} otherwise{goto(runTestIdle)}

      }

    }

  }

}
