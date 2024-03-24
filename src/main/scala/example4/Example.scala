package explorer.example4

import chisel3._
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp}
//import freechips.rocketchip.interrupts.{IntXbar, IntSinkNode, IntSourceNode, IntSourcePortSimple, IntSinkPortSimple}

import org.chipsalliance.cde.config.{Config, Parameters}

import explorer.example2.{InterruptsGen, NEdgeSink, NEdgeSource}
import explorer.example3.{DUT, 
  CounterWidth
}

import freechips.rocketchip.util.ClockDivider3
import freechips.rocketchip.diplomacy.AsynchronousCrossing
import freechips.rocketchip.subsystem.CrossingWrapper

class TopModule(implicit p: Parameters = Parameters.empty) extends LazyModule {
  val src  = LazyModule(new InterruptsGen()(new Config((_,_,_) => {case NEdgeSource => 1})))
  
  val island = LazyModule(new CrossingWrapper(AsynchronousCrossing())) //Clock Domain Island
  //By Wrapping "dut" with "island" we ensure that signals do not cross clock domain without proper synchronization.
  val dut = island{ LazyModule(new DUT()(new Config((_,_,_) => {case NEdgeSink => 1}))) }

  island.crossIntIn(dut.sink.node) := src.node

  //TODO Add clock-crossing for the dut.module.io.interrupt_count
  override lazy val module = new TopModuleImp(this)
}

class TopModuleImp(outer: TopModule) extends LazyModuleImp(outer) {
  val io = IO(new Bundle {
    val in  = Input(Bool())
    val intCount = Output(UInt((p(CounterWidth)).W))
  })
  dontTouch(io)
  val clk = Wire(Clock()).suggestName("DUT_CLK"); dontTouch(clk)
  val clockBy3 = Module(new ClockDivider3)
  clockBy3.io.clk_in := clock
  clk := clockBy3.io.clk_out
  outer.island.module.clock := clk
  outer.src.interrupts.head := io.in
  io.intCount := 0.U
  //io.intCount := outer.dut.module.io.interrupt_count //this is not available due to island-wrapping
}


