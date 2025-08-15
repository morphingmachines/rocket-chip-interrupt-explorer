package explorer.example2

import chisel3._
import freechips.rocketchip.interrupts.{
  IntNameNode,
  IntRange,
  IntSinkNode,
  IntSinkParameters,
  IntSinkPortParameters,
  IntSourceNode,
  IntSourceParameters,
  IntSourcePortParameters,
}
import org.chipsalliance.cde.config.{Config, Field, Parameters}
import org.chipsalliance.diplomacy.ValName
import org.chipsalliance.diplomacy.lazymodule.{InModuleBody, LazyModule, LazyModuleImp}
case object NEdgeSource extends Field[Int](2)
case object NEdgeSink   extends Field[Int](2)

class InterruptsGen(implicit p: Parameters) extends LazyModule {
  val intSourcePortParams = IntSourcePortParameters(sources =
    Seq(IntSourceParameters(range = IntRange(1))),
  ) // Edge with only one interrupt signal; Vec[Bool] of length 1
  val numEdges = p(NEdgeSource)
  val node = IntSourceNode(portParams = Seq.fill(numEdges)(intSourcePortParams))(
    ValName("GenNode"),
  ) // numEdges Edges with each of type Vec[Bool] of length 1
  val interrupts           = InModuleBody(node.makeIOs()) // Add source node IOs to the moduleImp
  override lazy val module = new InterruptsGenImp(this)
}

class InterruptsGenImp(outer: InterruptsGen) extends LazyModuleImp(outer) {}

class InterruptsRecv(implicit p: Parameters) extends LazyModule {
  val intSinkPortParams    = IntSinkPortParameters(sinks = Seq(IntSinkParameters()))
  val numEdges             = p(NEdgeSink)
  val node                 = IntSinkNode(portParams = Seq.fill(numEdges)(intSinkPortParams))(ValName(s"RecvNode_$numEdges"))
  val interrupts           = InModuleBody(node.makeIOs()) // Add sink node IOs to the moduleImp
  override lazy val module = new InterruptsRecvImp(this)
}

class InterruptsRecvImp(outer: InterruptsRecv) extends LazyModuleImp(outer) {}

/** Module that instantiates InterruptsGen and InterruptsRecv connected to each other with 'nEdges+1' edges. We can
  * connecting/drawing edges between nodes using three different binding operators (:=, :=*, and :*=).
  */
abstract class TopModule(val nEdges: Int)(implicit p: Parameters = Parameters.empty) extends LazyModule {

  val src   = LazyModule(new InterruptsGen()(new Config((_, _, _) => { case NEdgeSource => nEdges + 1 })))
  val sinkA = LazyModule(new InterruptsRecv()(new Config((_, _, _) => { case NEdgeSink => nEdges })))
  val sinkB = LazyModule(new InterruptsRecv()(new Config((_, _, _) => { case NEdgeSink => 1 })))

  sinkB.node := src.node
  override lazy val module = new TopModuleImp(this)
}

trait WithBindOnce { this: TopModule =>
  // Draw edges using := (BIND_ONCE) operator. To draw 'N' edges call this operator 'N' times.
  for (_ <- 0 until nEdges) {
    sinkA.node := src.node // An edge is drawn/connected from RHS-node to the LHS-node.
  }
}

trait WithBindQuery { this: TopModule =>

  /** Draw edges using :=* operator. BIND_QUERY operator w.r.t LHS-node. BIND_STAR operator w.r.t RHS-node. Number of
    * edges drawn is determined by number of outward-edges missing in the RHS-node in the partially elaborated
    * diplomatic graph.
    */
  sinkA.node :=* src.node
}

trait WithBindStar { this: TopModule =>

  /** Draw edges using :*= operator. BIND_STAR operator w.r.t LHS-node. BIND_QUERY operator w.r.t RHS-node. Number of
    * edges drawn is determined by number of inward-edges missing in the LHS-node in the partially elaborated diplomatic
    * graph.
    */
  sinkA.node :*= src.node
}

trait WithBindFlex { this: TopModule =>
  val myAdapter = IntNameNode("myAdapter")

  /** IntNameNode is an Identity node in which the number of inward and outward edges are same.
    */
  sinkA.node :*=* myAdapter :*= src.node
  // sinkA.node :*=* myAdapter :*= src.node
  // sinkA.node :=* myAdapter :*=* src.node
  // sinkA.node :*= myAdapter :*=* src.node

  /*
  for( _ <- 0 until nEdges){
    sinkA.node := myAdapter
    myAdapter := src.node
  }
   */

  // ------ Below statements throw compilation error - WHY?? -------------

  /*
  /* This is BIND-FLEX operator, as myAdapter has "nEdges" input edges, which enforces that it has "nEdges" output edges.
   * Since myAdapter output edges are fixed, the FLEX-BIND should resolve the edges to sinkA.node to myAdapter as dictated by
   * number of output edges required by myAdapter.
   */
  sinkA.node :*=* myAdapter
  for( _ <- 0 until nEdges){
    myAdapter := src.node
  }
   */

}

class Method1(n: Int) extends TopModule(n) with WithBindOnce
class Method2(n: Int) extends TopModule(n) with WithBindQuery
class Method3(n: Int) extends TopModule(n) with WithBindStar
class Method4(n: Int) extends TopModule(n) with WithBindFlex

class TopModuleImp(outer: TopModule) extends LazyModuleImp(outer) {
  val n_in       = outer.src.node.out.map(x => x._2.source.num).fold(0)(_ + _)
  val sink_ports = outer.sinkA.node.in ++ outer.sinkB.node.in
  val n_out      = sink_ports.map(x => x._2.source.num).fold(0)(_ + _)
  val io = IO(new Bundle {
    val in  = Input(Vec(n_in, Bool()))
    val out = Output(Vec(n_out, Bool()))
  })

  io.out.zip(outer.sinkA.interrupts ++ outer.sinkB.interrupts).foreach { case (i, j) => i := j.head }
  io.in.zip(outer.src.interrupts).foreach { case (i, j) => j.head := i }
}
