package explorer.example3

import chisel3._

import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp}
//import freechips.rocketchip.interrupts.{IntXbar, IntSinkNode, IntSourceNode, IntSourcePortSimple, IntSinkPortSimple}
import freechips.rocketchip.interrupts.{
  //IntXbar
  IntNameNode
}
import org.chipsalliance.cde.config.{Config, Parameters}

import explorer.example2.{InterruptsGen, InterruptsRecv, NEdgeSink, NEdgeSource}

//Connect Interrupts to a XBar

class TopModule(implicit p: Parameters = Parameters.empty) extends LazyModule {
  val sink = LazyModule(new InterruptsRecv()(new Config((_,_,_) => {case NEdgeSink => 2})))
  val src  = LazyModule(new InterruptsGen()(new Config((_,_,_) => {case NEdgeSource => 2})))
  val adapter = IntNameNode("sink_adapter")
  //val int_bus = LazyModule(new IntXbar)

  //sink.node :*= int_bus.intnode :=* src.node
  sink.node :*= adapter :*=* src.node

  override lazy val module = new TopModuleImp(this)
}

class TopModuleImp(outer: TopModule) extends LazyModuleImp(outer) {
  val n_in = outer.src.node.out.map{x => x._2.source.num}.fold(0){_ + _} 
  val n_out = outer.sink.node.in.map{x => x._2.source.num}.fold(0){_ + _} 
  val io = IO(new Bundle {
    val in  = Input(Vec(n_in,Bool()))
    val out = Output(Vec(n_out,Bool()))
  })

  io.out                        := outer.sink.module.io.interrupt
  outer.src.module.io.interrupt := io.in
}
