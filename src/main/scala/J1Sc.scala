/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Create Date:    Tue Sep 20 15:07:10 CEST 2016 
 * Module Name:    J1Sc - Toplevel
 * Project Name:   J1Sc - A simple J1 implementation
 *
 * Hash: bc807384d0d2b794942f61aab9c8f504ae031538
 * Date: Wed Sep 21 00:06:08 2016 +0200
 */
import spinal.core._
import spinal.lib._

class J1Sc (wordSize     : Int = 32,
            stackDepth   : Int = 256,
            addrWidth    : Int = 13,
            startAddress : Int = 0 ) extends Component {

  // I/O ports
  val io = new Bundle {

    // Address of next instruction
    //val instrAdr = out (UInt(addrWidth bits))

    // Next instruction
    val instr = in (Bits(wordSize bits))

  }.setName("")

  // Data stack pointer (init to first entry)
  val dStackPtr = Reg(UInt(log2Up(stackDepth) bits)) init(0)  

  // Write enable signal for data stack
  val dStackWrite : Bool;

  // Return stack pointer (init to first entry)
  val rStackPtr = Reg(UInt(log2Up(stackDepth) bits)) init(0)

  // Write enable for return stack
  val rStackWrite : Bool;

  // Data and return stack (do not init, hence undefined value after startup) 
  val dStack = Mem(Bits(wordSize bits), wordCount = stackDepth)
  val rStack = Mem(Bits(wordSize bits), wordCount = stackDepth)

  // Top of stack (do not init, hence undefined value after startup)
  val dtos = Reg(Bits(wordSize bits))

  // Next of data stack
  val dnos = dStack.readAsync(address = dStackPtr);

  // Data stack write port
  dStack.write(enable  = dStackWrite,
               address = dStackPtr,
               data    = )

  // Return stack write port
  rStack.write(enable  = rStackWrite,
               address = rStackPtr,
               data    = );

  // Top of return stack
  val rtos = rStack.readAsync(address = rStackPtr);

  // Programm counter (PC)
  val pc = Reg(UInt(addrWidth bits)) init(startAddress);

  // Instruction to be excuted
  val instr = io.instr

  // Instruction decoder (including ALU operations)
  switch(instr(instr.high downto (instr.high - 8) + 1)) {

    // Literal
    is(M"1-------") {dtos = instr(instr.high - 1 downto 0).resize(wordSize)}

    // Jump instruction (do not change dtos)
    is(M"000-----") {dtos = dtos}

    // Call instruction (do not change dtos)
    is(M"000-----") {dtos = dtos}

    // Conditional jump (pop the 0 at dtos)
    is(M"001-----") {dtos = dnos}

    // ALU operations using dtos and dnos
    is(M"011-0000") {dtos = dtos}
    is(M"011-0001") {dtos = dnos}

    // Arithmetic and logical operations (ALU)
    is(M"011-0010") {dtos = (dtos.asUInt + dnos.asUInt).asBits}
    is(M"011-0011") {dtos = dtos & dnos}
    is(M"011-0100") {dtos = dtos | dnos}
    is(M"011-0101") {dtos = dtos ^ dnos}
    is(M"011-0110") {dtos = ~dtos}
    is(M"011-1001") {dtos = dtos >> dnos(log2Up(wordSize - 1) downto 0).asUInt}
    is(M"011-1010") {dtos = dtos << dnos(log2Up(wordSize - 1) downto 0).asUInt}
    is(M"011-1011") {dtos = rtos}

    // Set all bits of top of stack to false by default
    default {dtos = (default -> false)}

  }

  val funcTtoN  = (instr(6 downto 4).asUInt === 1) // Copy DTOS to DNOS
  val funcTtoR  = (instr(6 downto 4).asUInt === 2) // Copy DTOS to return stack
  val funcWrite = (instr(6 downto 4).asUInt === 3) 
  val funcIOW   = (instr(6 downto 4).asUInt === 4)
  val isALU     = (instr(instr.high downto (instr.high - 3) + 1) === B"b011");

  //assign mem_wr = !reboot & is_alu & func_write;
  //assign dout = st1;
  //assign io_wr = !reboot & is_alu & func_iow;

  //assign rstkD = (insn[13] == 1'b0) ? {{(`WIDTH - 14){1'b0}}, pc_plus_1, 1'b0} : st0;

  // Increment for data stack pointer
  val dStackPointerInc : SInt(log2Up(stackDepth) bits);

  // Handle update of data stack
  switch(instr(instr.high downto (instr.high - 3) + 1)) {

    // Literal (push value to data stack)
    is(M"1--") {dStackWrite = true; dStackPointerInc = 1}

    // Conditional jump (pop DTOS from data stack)
    is(M"001") {dStackWrite = false; dStackPointerInc = -1}

    // ALU instruction
    is(M"011"){dStackWrite = funcTtoN; dStackPointerInc = instr(1 downto 0).asSInt(addrWidth)}

    // Don't change the data stack by default
    default {dstackWrite = false; dStackPoinerInc = 0}

  }

  // Update the data stack pointer
  dStackPtr = dStackPtr + dStackPointerInc

  // Increment for data stack pointer
  val rStackPointerInc : SInt(log2Up(stackDepth) bits);

  // Handle update of return stack
  switch(instr(instr.high downto (instr.high - 3) + 1)) {

    // Call instruction (push return address to stack)
    is(M"010") {rStackWrite = true; rStackPointerInc = 1}

    // Conditional jump (maybe we have to push)
    is(M"011") {rStackWrite = funcTtoR; rStackPointerInc = instr(3 downto 2).asSInt(addrWidth)}

    // Don't change the return stack by default
    default {rStackWrite = false; rStackPointerInc = 0}

  }

  // Update the return stack pointer
  rStackPtr = rStackPtr + rStackPointerInc

  // Handle the PC 
  switch(instr(instr.high downto (instr.high - 4) + 1)) {

    // Check for jump, call and cond. jump instruction
    is(M"000_") {pc <= instr(adrWidth - 1 downto 0)}
    is(M"010_") {pc <= instr(adrWidth - 1 downto 0)}
    is(M"001_") {pc <= instr(adrWidth - 1 downto 0)}

    // Check for R -> PC field of an ALU instruction
    is(M"0111") {pc <= rtos}

    // By default goto next instruction
    default {pc <= pc + 1}

  }

}

object J1Sc {

  // Make the reset synchron
  val globalClockConfig = ClockDomainConfig (
    clockEdge        = RISING,
    resetKind        = SYNC,
    resetActiveLevel = HIGH
  )

  def main(args: Array[String]) {

    // Generate HDL files
    SpinalConfig(genVhdlPkg = false,
                 defaultConfigForClockDomains = globalClockConfig,
                 targetDirectory="gen/src/vhdl").generateVhdl(new J1Sc)
    SpinalConfig(genVhdlPkg = false,
                 defaultConfigForClockDomains = globalClockConfig,
                 targetDirectory="gen/src/verilog").generateVerilog(new J1Sc)

  }

}
