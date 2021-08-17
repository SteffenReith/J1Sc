/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Create Date:    Sat May 12 00:46:16 GMT+2 2018
 * Module Name:    SingleStepUnit - Implements a trigger mechanism for the single step mode
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._

class SingleStepUnit {

  val internal = new Bundle {

    // Enable the single step mode (registered)
    val enableSingleStep = in Bool()

    // Trigger the next step for exactly one time
    val triggerStep = in Bool()

    // Do a step (fetch the next instruction)
    val doStep = out Bool()

  }.setName("")

  // Register the step mode
  val stepMode = RegNext(internal.enableSingleStep) init(False)

  // Check whether we in step mode and if we have a rising edge for the trigger
  internal.doStep := !stepMode || internal.triggerStep.rise(False)

}
