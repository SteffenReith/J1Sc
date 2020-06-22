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

  // The write enable signal for the stack
  val dStackWriteEnable = Bool

  // Data stack pointer and next signal
  val dStackPtrN = UInt(cfg.dataStackIdxWidth bits)
  val dStackPtr  = Reg(UInt(cfg.dataStackIdxWidth bits)) init (0)

  def apply(stall : Bool, dtosN : Bits) : (Bits, Bits, UInt) = {

    // Change the dstack pointer only when the CPU is not stalled
    when(!stall) { dStackPtr := dStackPtrN }

    // Top of data stack and next value
    val dtos  = RegNext(dtosN) init(0)

    // Data stack with read and write port
    val stackMem = Mem(Bits(cfg.wordSize bits), wordCount = (1 << cfg.dataStackIdxWidth))
    stackMem.write(address = dStackPtrN,
                   data    = dtos,
                   enable  = dStackWriteEnable & !stall)
    val dnos = stackMem.readAsync(address = dStackPtr, readUnderWrite = writeFirst)

    // Return top and next of stack as a pair and the dstack pointer
    (dtos, dnos, dStackPtr)

  }

  def updateDStack(msb : Bool, instr : Bits, funcTtoN : Bool) : Unit = {

    // Increment for data stack pointer
    val dStackPtrInc = SInt(cfg.dataStackIdxWidth bits)

    // Handle the update of the data stack
    switch(msb ## instr(instr.high downto (instr.high - 3) + 1)) {

      // For a high call push the instruction (== memory access) and for a literal push the value to the data stack
      is (M"1_---", M"0_1--") {dStackWriteEnable := True; dStackPtrInc := 1}

      // Conditional jump (pop DTOS from data stack)
      is (M"0_001") {dStackWriteEnable := False; dStackPtrInc := -1}

      // ALU instruction (check for a possible push of data, ISA bug can be fixed by '| (instr(1 downto 0) === B"b01")')
      is (M"0_011") {dStackWriteEnable := funcTtoN; dStackPtrInc := instr(1 downto 0).asSInt.resized}

      // Don't change the data stack by default
      default {dStackWriteEnable := False; dStackPtrInc := 0}

    }

    // Calculate the new value of the data stack pointer
    dStackPtrN := (dStackPtr.asSInt + dStackPtrInc).asUInt

  }

}
