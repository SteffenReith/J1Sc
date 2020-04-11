/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Create Date:    Sat Apr 11 13:56:45 CEST 2020
 * Module Name:    J1Decoder - Decodes the current instruction to determine the dstack top for the next clock
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._

case class J1Decoder(cfg : J1Config) {

  def apply(pc : UInt, instr : Bits, dtos : Bits, dnos : Bits, aluResult : Bits) : Bits = {

    val dtosN = Bits(cfg.wordSize bits)

    // Instruction decoder
    switch (pc.msb ## instr(instr.high downto (instr.high - 3) + 1)) {

      // If there is a high call then push the instruction (== memory access) to the data stack
      is (M"1_---") {
        dtosN := instr
      }

      // Literal instruction (Push value)
      is (M"0_1--") {
        dtosN := instr(instr.high - 1 downto 0).resized
      }

      // Jump and call instruction (do not change dtos)
      is (M"0_000", M"0_010") {
        dtosN := dtos
      }

      // Conditional jump (pop a 0 at dtos by adjusting the dstack pointer)
      is (M"0_001") {
        dtosN := dnos
      }

      // Check for ALU operation
      is (M"0_011") {
        dtosN := aluResult
      }

      // Set all bits of top of stack to true by default
      default {
        dtosN := (default -> True)
      }

    }

    // Next top of data stack
    dtosN

  }

}
