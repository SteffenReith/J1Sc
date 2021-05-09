/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Create Date:    Tue Sep 20 15:07:10 CEST 2016
 * Module Name:    J1Core - CPU core (ALU, Decoder, Stacks, etc)
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 */
import spinal.core._

class J1Core(cfg : J1Config) extends Component {

  // Check the generic parameters
  assert(cfg.wordSize == 16, message = "ERROR: Only wordsize 16 was tested!")
  assert((cfg.wordSize - 3) >= cfg.adrWidth, message = "ERROR: The width of an address is too large")

  // Internally used signals
  val internal = new Bundle {

    // Signals for memory and io port
    val memWriteMode = out Bool
    val ioWriteMode  = out Bool
    val ioReadMode   = out Bool
    val extAdr       = out UInt(cfg.wordSize bits)
    val extToWrite   = out Bits(cfg.wordSize bits)
    val toRead       = in  Bits(cfg.wordSize bits)

    // Signal to stall the CPU
    val stall = in Bool

    // Interface for the interrupt system
    val irq    = in Bool
    val intVec = in Bits (cfg.adrWidth bits)

    // I/O port for instructions
    val nextInstrAdr = out UInt(cfg.adrWidth bits)
    val memInstr     = in Bits(cfg.wordSize bits)

  }.setName("")

  // The stall signal is allowed to be pruned (if we have no JTAG)
  internal.stall.allowPruning()

  // Used for checking whether a reset is active
  val clrActive = ClockDomain.current.isResetActive

  // Create the program counter (the MSB is used to control the stacks, hence make it one bit larger)
  val pcObj          = J1PC(cfg)
  val pcN            = UInt(cfg.adrWidth + 1 bits)
  val (pc, returnPC) = pcObj(pcN, clrActive, internal.irq)

  // Check status and inject nop (stall mode) or call-instruction (interrupt mode) when needed
  val stateSelect = internal.stall ## internal.irq
  val instr = stateSelect.mux(B"b00" -> internal.memInstr,                                   // Normal mode
                              B"b01" -> B"b010" ## internal.intVec.resize(cfg.wordSize - 3), // Interrupt mode
                              B"b10" -> J1Config.instrNOP(cfg.wordSize),                     // Stall mode
                              B"b11" -> J1Config.instrNOP(cfg.wordSize))                     // Stall overrides interrupt

  // Create the data stack
  val dStack                  = J1DStack(cfg)
  val dtosN                   = Bits(cfg.wordSize bits)
  val (dtos, dnos, dStackPtr) = dStack(internal.stall, dtosN)

  // Set next value for RTOS (check call / interrupt or T -> R ALU instruction)
  val rtosN = Mux(!instr(instr.high - 3 + 1), (returnPC ## B"b0").resized, dtos)

  // Create the return stack
  val rStack = J1RStack(cfg)
  val rtos   = rStack(internal.stall, rtosN)

  // Create an ALU (the AluOp is taken out of the instruction)
  val alu = J1Alu(cfg)
  val aluResult = alu(instr, dtos, dnos, dStackPtr, rtos, internal.toRead)

  // Decode instruction and calculate next top of data stack
  dtosN := J1Decoder(cfg)(pc, instr, dtos, dnos, aluResult)

  // Internal control signals
  val funcTtoN     = instr(6 downto 4) === B"001" // Copy DTOS to DNOS
  val funcTtoR     = instr(6 downto 4) === B"010" // Copy DTOS to return stack
  val funcWriteMem = instr(6 downto 4) === B"011" // Write to RAM
  val funcWriteIO  = instr(6 downto 4) === B"100" // I/O write operation
  val funcReadIO   = instr(6 downto 4) === B"101" // I/O read operation
  val isALU        = !pc.msb && (instr(instr.high downto (instr.high - 3) + 1) === B"b011") // ALU operation

  // Control signals used for external memory
  internal.memWriteMode := !clrActive && isALU && funcWriteMem
  internal.ioWriteMode  := !clrActive && isALU && funcWriteIO
  internal.ioReadMode   := !clrActive && isALU && funcReadIO
  internal.extAdr       := dtosN.asUInt
  internal.extToWrite   := dnos

  // Update the data stack internals
  dStack.update(pc.msb, instr, funcTtoN)

  // Update the return stack internals
  rStack.update(pc.msb, instr, funcTtoR)

  // Create the PC update logic
  pcN := pcObj.updatePC(stall     = internal.stall,
                        clrActive = clrActive,
                        instr     = instr,
                        dtos      = dtos,
                        rtos      = rtos)

  // Use next PC as address of instruction memory (do not use the MSB)
  internal.nextInstrAdr := pcN(pcN.high - 1 downto 0)

}
