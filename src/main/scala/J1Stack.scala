/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Create Date:    Mon Jun 22 23:54:55 CEST 2020 
 * Module Name:    J1Stack - Holds member used for data- and  return stack
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._

class J1Stack(idxWidth : Int) extends Area {

  // Enable signal for writing to the stack
  val stackWriteEnable = Bool()

  // Stack pointer and next signal for the data stack
  val stackPtrN = UInt(idxWidth bits)
  val stackPtr  = Reg(UInt(idxWidth bits)) init (0)

}
