package explorer.example1

import chisel3._
import freechips.rocketchip.interrupts.{
  IntRange,
  IntSinkNode,
  IntSinkParameters,
  IntSinkPortParameters,
  IntSourceNode,
  IntSourceParameters,
  IntSourcePortParameters,
}
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.lazymodule.{InModuleBody, LazyModule, LazyModuleImp}

class InterruptsGen(implicit p: Parameters) extends LazyModule {
  // Comprises one source node with one outward edge
  val intSourcePortParams = IntSourcePortParameters(sources =
    Seq(IntSourceParameters(range = IntRange(0, 2))),
  ) // Edge with two interrupt signals; Vec[Bool] of length 2
  val node                 = IntSourceNode(portParams = Seq(intSourcePortParams)) // portParams length=1 for one edge.
  val interrupts           = InModuleBody(node.makeIOs())                         // Add source node IOs to the moduleImp
  override lazy val module = new InterruptsGenImp(this)
}

class InterruptsGenImp(outer: InterruptsGen) extends LazyModuleImp(outer) {}

class InterruptsRecv(implicit p: Parameters) extends LazyModule {
  // Comprises one sink node with one inward edge
  val intSinkPortParams    = IntSinkPortParameters(sinks = Seq(IntSinkParameters()))
  val node                 = IntSinkNode(portParams = Seq(intSinkPortParams)) // portParams length=1 for one edge.
  val interrupts           = InModuleBody(node.makeIOs())                     // Add sink node IOs to the moduleImp
  override lazy val module = new InterruptsRecvImp(this)
}

class InterruptsRecvImp(outer: InterruptsRecv) extends LazyModuleImp(outer) {}

class TopModule(implicit p: Parameters = Parameters.empty) extends LazyModule {
  val sink = LazyModule(new InterruptsRecv)
  val src  = LazyModule(new InterruptsGen)

  /** Connects Inward edge of the InterruptRecv to Outward edge of the InterruptGen. ":=" is a BIND_ONCE operator, draws
    * an edge from RHS-node to the LSH-node.
    */
  sink.node := src.node

  override lazy val module = new TopModuleImp(this)
}

class TopModuleImp(outer: TopModule) extends LazyModuleImp(outer) {
  val n = outer.src.node.out(0)._2.source.num // number of interrupt signals
  val io = IO(new Bundle {
    val in  = Input(Vec(n, Bool()))
    val out = Output(Vec(n, Bool()))
  })

  outer.sink.interrupts.foreach(i => io.out := i)
  outer.src.interrupts.foreach(i => i := io.in)
}
