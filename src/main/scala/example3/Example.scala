package explorer.example3

import chisel3._
import chisel3.util.Counter
import explorer.example2.{InterruptsGen, InterruptsRecv, NEdgeSink, NEdgeSource}
import freechips.rocketchip.interrupts.{IntSyncAsyncCrossingSink, IntSyncCrossingSource}
import freechips.rocketchip.util.{ClockDivider3, SynchronizerShiftReg}
import org.chipsalliance.cde.config.{Config, Field, Parameters}
import org.chipsalliance.diplomacy.lazymodule.{LazyModule, LazyModuleImp}

case object CounterWidth extends Field[Int](10)
//Connect Interrupts to a XBar

class DUT(implicit p: Parameters) extends LazyModule {
  val sink = LazyModule(new InterruptsRecv())

  override lazy val module = new DUTImp(this)
}

class DUTImp(outer: DUT)(implicit p: Parameters) extends LazyModuleImp(outer) {
  val io = IO(new Bundle {
    val interrupt_count = Output(UInt(p(CounterWidth).W))
  })

  val detectInterrupt = Module(new EdgeDetector(false))
  io.interrupt_count        := Counter(detectInterrupt.io.detect_out, 1 << p(CounterWidth))._1
  detectInterrupt.io.sig_in := outer.sink.interrupts.head.head // Only zeroth indexed signal is used
  printf(cf"interrupt_count :${io.interrupt_count}")
}

class TopModule(implicit p: Parameters = Parameters.empty) extends LazyModule {
  val src        = LazyModule(new InterruptsGen()(new Config((_, _, _) => { case NEdgeSource => 1 })))
  val adapterSrc = LazyModule(new IntSyncCrossingSource(alreadyRegistered = false))

  val adapterSink = LazyModule(new IntSyncAsyncCrossingSink(3))
  val dut         = LazyModule(new DUT()(new Config((_, _, _) => { case NEdgeSink => 1 })))

  // "src" and "adapterSrc" in Clock-domain 1
  // "adapterSink" and "dut" in Clock-domain 2
  // example4 shows how to do this in a more safe way.
  dut.sink.node := adapterSink.node := adapterSrc.node := src.node

  override lazy val module = new TopModuleImp(this)
}

class TopModuleImp(outer: TopModule)(implicit p: Parameters) extends LazyModuleImp(outer) {
  val io = IO(new Bundle {
    val in       = Input(Bool())
    val intCount = Output(UInt(p(CounterWidth).W))
  })

  val clockBy3 = Module(new ClockDivider3)
  val clk      = Wire(Clock()).suggestName("DUT_CLK"); dontTouch(clk)
  clockBy3.io.clk_in := clock
  clk                := clockBy3.io.clk_out

  val synchronizer = Module(new SynchronizerShiftReg(p(CounterWidth), 3)) // Synchronous with default clock

  // Different clock for dut and adapterSink
  outer.dut.module.clock         := clk
  outer.adapterSink.module.clock := clk
  synchronizer.io.d              := outer.dut.module.io.interrupt_count // Synchronous with "DUT_CLK"
  outer.src.interrupts.head.head := io.in                               // Synchronous with default clock
  io.intCount                    := synchronizer.io.q                   // Synchronous with default clock
}

class EdgeDetector(falling_not_rising: Boolean) extends Module {
  val io = IO(new Bundle {
    val sig_in     = Input(Bool())
    val detect_out = Output(Bool())
  })

  val sig_r = RegNext(io.sig_in)

  if (falling_not_rising) {
    io.detect_out := sig_r & !io.sig_in
  } else {
    io.detect_out := !sig_r & io.sig_in
  }
}
