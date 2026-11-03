/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Create Date:    Thu Jun 25 12:55:38 CEST 2020 
 * Module Name:    J1DStack - The return stack
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._

case class J1RStack(cfg : J1Config) extends J1Stack(cfg.dataStackIdxWidth)  {

  def apply(stall : Bool, rtosN : Bits) : Bits = {

    // Change the stack pointer only when the CPU is not stalled
    when(!stall) { stackPtr := stackPtrN }

    // Return stack with write and read port
    val stackMem = Mem(Bits(cfg.wordSize bits), wordCount = (1 << cfg.returnStackIdxWidth))
    stackMem.write(address = stackPtrN,
                   data    = rtosN,
                   enable  = stackWriteEnable & !stall)
    val rtos = stackMem.readAsync(address = stackPtr, readUnderWrite = writeFirst)

    // Return the top of stack
    rtos

  }

  def updateRStack(msb : Bool, instr : Bits, funcTtoR : Bool) : Unit = {

    // Increment for return stack pointer
    val stackPtrInc = SInt(cfg.returnStackIdxWidth bits)

    // Handle the update of the return stack
    switch(msb ## instr(instr.high downto (instr.high - 3) + 1)) {

      // When we do a high call (the msb of the PC is set) do a pop of return address
      is(M"1_---") {stackWriteEnable := False; stackPtrInc := -1}

      // Call instruction or interrupt (push return address to stack)
      is(M"0_010") {stackWriteEnable := True; stackPtrInc := 1}

      // Conditional jump (maybe we have to push)
      is(M"0_011") {stackWriteEnable := funcTtoR; stackPtrInc := instr(3 downto 2).asSInt.resized}

      // Don't change the return stack by default
      default {stackWriteEnable := False; stackPtrInc := 0}

    }

    // Update the return stack pointer
    stackPtrN := (stackPtr.asSInt + stackPtrInc).asUInt

  }

}
