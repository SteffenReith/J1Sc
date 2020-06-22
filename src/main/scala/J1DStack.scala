/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Create Date:    Fri Jun 19 10:19:44 CEST 2020
 * Module Name:    J1DStack - The data stack
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._

case class J1DStack(cfg : J1Config) {

  def updateDStackPtr(msb : Bool, instr : Bits, funcTtoN : Bool ) : (Bool, SInt) = {

    // The write enable signal for the stack
    val dStackWriteEnable = Bool

    // Increment for data stack pointer
    val dStackPtrInc = SInt(cfg.dataStackIdxWidth bits)

    // Handle the update of the data stack
    switch(msb ## instr(instr.high downto (instr.high - 3) + 1)) {

      // For a high call push the instruction (== memory access) and for a literal push the value to the data stack
      is(M"1_---", M"0_1--") {dStackWriteEnable := True; dStackPtrInc := 1}

      // Conditional jump (pop DTOS from data stack)
      is(M"0_001") {dStackWriteEnable := False; dStackPtrInc := -1}

      // ALU instruction (check for a possible push of data, ISA bug can be fixed by '| (instr(1 downto 0) === B"b01")')
      is(M"0_011"){dStackWriteEnable := funcTtoN; dStackPtrInc := instr(1 downto 0).asSInt.resized}

      // Don't change the data stack by default
      default {dStackWriteEnable := False; dStackPtrInc := 0}

    }

    // Return enable and increment
    (dStackWriteEnable, dStackPtrInc)

  }

}