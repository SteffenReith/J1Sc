package j1sc

import spinal.core._
import spinal.core.sim._
import org.scalatest.funsuite.AnyFunSuite

// =====================================================================
//  Regresja ISA po porcie na SpinalHDL 1.12: male programy w ROM-ie,
//  wynik obserwowany jako zapis na magistrali I/O (cpuBus). Kodowanie
//  instrukcji i komentarze jak w J1ISA16 (J1Config.scala).
// =====================================================================
object J1IsaTest {
  def push(v : Int) = { require(v >= 0 && v < 0x8000); 0x8000 | v }
  def jmp(a : Int)  = 0x0000 | a
  def jz(a : Int)   = 0x2000 | a
  def call(a : Int) = 0x4000 | a
  val addDrop   = 0x6203   // T+N, dstack -1
  val ioWrNip   = 0x6043   // N -> io[T], dstack -1
  val ioRd      = 0x6D50   // T <- io[T]
  val pop       = 0x6103
  val ret       = 0x608C
  val nop       = 0x6000

  val adrWidth = 8

  def cfg(prog : Seq[Int]) = J1Config(
    wordSize            = 16,
    dataStackIdxWidth   = 5,
    returnStackIdxWidth = 4,
    hasJtag             = false,
    jtagConfig          = null,
    timerConfig         = TimerConfig.default,
    irqConfig           = IRQCtrlConfig.default,
    adrWidth            = adrWidth,
    numOfRAMs           = 2,
    startAddress        = 0,
    bootCode            = () => (prog ++ Seq.fill((1 << adrWidth) - prog.size)(0)).map(B(_, 16 bits)).toList)

  val spinal = SpinalConfig(defaultConfigForClockDomains =
    ClockDomainConfig(clockEdge = RISING, resetKind = SYNC, resetActiveLevel = HIGH))
}

class J1IsaTest extends AnyFunSuite {
  import J1IsaTest._

  /** Uruchamia program, zwraca zapisy I/O (adres, dane). readData = ioIn. */
  def ioWrites(name : String, prog : Seq[Int], ioIn : Int = 0, cycles : Int = 200) : Seq[(Int, Int)] = {
    var writes = Vector.empty[(Int, Int)]
    SimConfig.withConfig(spinal).withVerilator.workspacePath("simWorkspace").workspaceName(name)
      .compile(new J1(cfg(prog))).doSim(name) { d =>
        d.internal.stall  #= false
        d.internal.irq    #= false
        d.internal.intVec #= 0
        d.bus.cpuBus.readData #= ioIn
        d.clockDomain.forkStimulus(10)
        d.clockDomain.assertReset()
        d.clockDomain.waitSampling(4)
        d.clockDomain.deassertReset()
        for (_ <- 0 until cycles) {
          d.clockDomain.waitSampling()
          val b = d.bus.cpuBus
          if (b.enable.toBoolean && b.writeMode.toBoolean)
            writes :+= (b.address.toInt -> b.writeData.toInt)
        }
      }
    writes
  }

  test("alu: 7 + 3 -> io[0x40]") {
    val prog = Seq(push(7), push(3), addDrop, push(0x40), ioWrNip, pop, jmp(6))
    assert(ioWrites("isa_alu", prog) == Seq(0x40 -> 10))
  }

  test("call / ret") {
    val prog = Seq(call(6), push(0x41), ioWrNip, pop, jmp(4), nop,
                   push(0x55), ret)
    assert(ioWrites("isa_call", prog) == Seq(0x41 -> 0x55))
  }

  test("skok warunkowy (jz)") {
    val prog = Seq(push(0), jz(4), push(0xBA), jmp(5),
                   push(0x77), push(0x42), ioWrNip, pop, jmp(8))
    assert(ioWrites("isa_jz", prog) == Seq(0x42 -> 0x77))
  }

  test("odczyt I/O -> zapis I/O") {
    val prog = Seq(push(0x43), ioRd, ioRd, push(0x44), ioWrNip, pop, jmp(6))
    assert(ioWrites("isa_ioread", prog, ioIn = 0x1234) == Seq(0x44 -> 0x1234))
  }
}
