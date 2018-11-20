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

class JTAG(jtagConfig : JTAGConfig) extends Component {

  // Signals used for the JTAG interface
  val jtagIO = new Bundle {

    // JTAG data input
    val tdi  = in Bool

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

      // Handle the states related to the data register
      selectDRScan.whenIsActive{when(jtagIO.tms) {goto(selectIRScan)} otherwise{goto(captureDR)}}
      captureDR.whenIsActive{when(jtagIO.tms) {goto(exit1DR)} otherwise{goto(shiftDR)}}
      shiftDR.whenIsActive{when(jtagIO.tms) {goto(exit1DR)} otherwise{goto(shiftDR)}}
      exit1DR.whenIsActive{when(jtagIO.tms) {goto(updateDR)} otherwise{goto(pauseDR)}}
      pauseDR.whenIsActive{when(jtagIO.tms) {goto(exit2DR)} otherwise{goto(pauseDR)}}
      exit2DR.whenIsActive{when(jtagIO.tms) {goto(updateDR)} otherwise{goto(shiftDR)}}
      updateDR.whenIsActive{when(jtagIO.tms) {goto(selectDRScan)} otherwise{goto(runTestIdle)}}

      // Handle the states related to the instruction register
      selectIRScan.whenIsActive{when(jtagIO.tms) {goto(testLogicReset)} otherwise{goto(captureIR)}}
      captureIR.whenIsActive{when(jtagIO.tms) {goto(exit1IR)} otherwise{goto(shiftIR)}}
      shiftIR.whenIsActive{when(jtagIO.tms) {goto(exit1IR)} otherwise{goto(shiftIR)}}
      exit1IR.whenIsActive{when(jtagIO.tms) {goto(updateIR)} otherwise{goto(pauseIR)}}
      pauseIR.whenIsActive{when(jtagIO.tms) {goto(exit2IR)} otherwise{goto(pauseIR)}}
      exit2IR.whenIsActive{when(jtagIO.tms) {goto(updateIR)} otherwise{goto(shiftIR)}}
      updateIR.whenIsActive{when(jtagIO.tms) {goto(selectDRScan)} otherwise{goto(runTestIdle)}}

    }

  }

}