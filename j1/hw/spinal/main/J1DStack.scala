/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Create Date:    Fri Jun 19 10:19:44 CEST 2020
 * Module Name:    J1DStack - The data stack
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._

case class J1DStack(cfg : J1Config) extends J1Stack(cfg.dataStackIdxWidth) {

  // Enable signal for writing to the stack
  //override val stackWriteEnable = Bool

  // Stack pointer and next signal for the data stack
  //val stackPtrN = UInt(cfg.dataStackIdxWidth bits)
  //val stackPtr  = Reg(UInt(cfg.dataStackIdxWidth bits)) init (0)

  def apply(stall : Bool, dtosN : Bits) : (Bits, Bits, UInt) = {

    // Change the stack pointer only when the CPU is not stalled
    when(!stall) { stackPtr := stackPtrN }

    // Top of stack and next value
    val dtos  = RegNext(dtosN) init(0)

    // Stack memory with read and write port
    val stackMem = Mem(Bits(cfg.wordSize bits), wordCount = (1 << cfg.dataStackIdxWidth))
    stackMem.write(address = stackPtrN,
                   data    = dtos,
                   enable  = stackWriteEnable & !stall)
    val dnos = stackMem.readAsync(address = stackPtr, readUnderWrite = writeFirst)

    // Return top and next of stack as a pair and the dstack pointer
    (dtos, dnos, stackPtr)

  }

  def updateDStack(msb : Bool, instr : Bits, funcTtoN : Bool) : Unit = {

    // Increment for data stack pointer
    val stackPtrInc = SInt(cfg.dataStackIdxWidth bits)

    // Handle the update of the data stack
    switch(msb ## instr(instr.high downto (instr.high - 3) + 1)) {

      // For a high call push the instruction (== memory access) and for a literal push the value to the data stack
      is (M"1_---", M"0_1--") {stackWriteEnable := True; stackPtrInc := 1}

      // Conditional jump (pop DTOS from data stack)
      is (M"0_001") {stackWriteEnable := False; stackPtrInc := -1}

      // ALU instruction (check for a possible push of data, ISA bug can be fixed by '| (instr(1 downto 0) === B"b01")')
      is (M"0_011") {stackWriteEnable := funcTtoN; stackPtrInc := instr(1 downto 0).asSInt.resized}

      // Don't change the data stack by default
      default {stackWriteEnable := False; stackPtrInc := 0}

    }

    // Calculate the new value of the data stack pointer
    stackPtrN := (stackPtr.asSInt + stackPtrInc).asUInt

  }

}
