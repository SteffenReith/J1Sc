/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Create Date:    Fri Apr  3 21:34:07 CEST 2020
 * Module Name:    J1Alu - The ALU for J1Sc
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import com.sun.xml.internal.ws.encoding.MtomCodec.MtomXMLStreamReaderEx
import spinal.core._

case class J1Alu(cfg : J1Config) {

  // Slice the ALU operation out of an instruction
  def getALUOp(instr : Bits) = instr((instr.high - 4) downto ((instr.high - 8) + 1))

  // Create and execute a ALU operation
  def apply(instr : Bits, dtos : Bits, dnos : Bits, dStackPtr : UInt, rtos : Bits, toRead : Bits) = {

    // Calculate difference (- dtos + dnos) and sign to be reused multiple times
    val difference = dnos.resize(cfg.wordSize + 1).asSInt - dtos.resize(cfg.wordSize + 1).asSInt
    val nosIsLess  = (dtos.msb ^ dnos.msb) ? dnos.msb | difference.msb

    // Calculate the ALU result (mux all possible cases)
    getALUOp(instr).mux(

      B"0000" -> dtos,
      B"0001" -> dnos,

      // Arithmetic and logical operations
      B"0010" -> (dtos.asUInt + dnos.asUInt).asBits,
      B"1100" -> difference.resize(cfg.wordSize).asBits,
      B"0011" -> (dtos & dnos),
      B"0100" -> (dtos | dnos),
      B"0101" -> (dtos ^ dnos),
      B"0110" -> (~dtos),
      B"1001" -> (dtos(dtos.high) ## dtos(dtos.high downto 1).asUInt),
      B"1010" -> (dtos(dtos.high - 1 downto 0) ## B"b0"),

      // Push rtos to dtos
      B"1011" -> rtos,

      // Compare operations (equal, dtos > dnos, signed and unsigned)
      B"0111" -> B(cfg.wordSize bits, default -> (difference === 0)),
      B"1000" -> B(cfg.wordSize bits, default -> nosIsLess),
      B"1111" -> B(cfg.wordSize bits, default -> difference.msb),

      // Memory / IO read operations
      B"1101" -> toRead,

      // Misc operations (depth of dstack)
      B"1110" -> dStackPtr.resize(cfg.wordSize bits).asBits)

  }

}
