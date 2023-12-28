package explorer.example2

import chisel3._

import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp, ValName}
//import freechips.rocketchip.interrupts.{IntXbar, IntSinkNode, IntSourceNode, IntSourcePortSimple, IntSinkPortSimple}
import freechips.rocketchip.interrupts.{
  IntRange,
  IntSourceParameters,
  IntSourcePortParameters,
  IntSourceNode,
  IntSinkParameters,
  IntSinkPortParameters,
  IntSinkNode,
  IntNameNode
}
import org.chipsalliance.cde.config.{Config, Field, Parameters}

case object NEdgeSource extends Field[Int](2)
case object NEdgeSink   extends Field[Int](2)

//Connect Interrupts to a XBar
class InterruptsGen(implicit p: Parameters) extends LazyModule {
  val intSourcePortParams  = IntSourcePortParameters(sources = Seq(IntSourceParameters(range = IntRange(1))))
  val numEdges             = p(NEdgeSource)
  val node                 = IntSourceNode(portParams = Seq.fill(numEdges)(intSourcePortParams))(ValName("GenNode"))
  override lazy val module = new InterruptsGenImp(this)
}

class InterruptsGenImp(outer: InterruptsGen) extends LazyModuleImp(outer) {
  val n = outer.node.out.map(x => x._2.source.num).fold(0)(_ + _)
  val io = IO(new Bundle {
    val interrupt = Input(Vec(n, Bool()))
  })

  outer.node.out.map(x => x._1).flatten.zip(io.interrupt).foreach { case (node_port, module_port) =>
    node_port := module_port
  }
}

class InterruptsRecv(implicit p: Parameters) extends LazyModule {
  val intSinkPortParams    = IntSinkPortParameters(sinks = Seq(IntSinkParameters()))
  val numEdges             = p(NEdgeSink)
  val node                 = IntSinkNode(portParams = Seq.fill(numEdges)(intSinkPortParams))(ValName(s"RecvNode_$numEdges"))
  override lazy val module = new InterruptsRecvImp(this)
}

class InterruptsRecvImp(outer: InterruptsRecv) extends LazyModuleImp(outer) {
  val n = outer.node.in.map(x => x._2.source.num).fold(0)(_ + _)
  val io = IO(new Bundle {
    val interrupt = Output(Vec(n, Bool()))
  })
  outer.node.in.map(x => x._1).flatten.zip(io.interrupt).foreach { case (node_port, module_port) =>
    module_port := node_port
  }
}

/** Module that instantiates InterruptsGen and InterruptsRecv connected to each other with 'nEdges+1' edges. We can
  * connecting/drawing edges between nodes using three different binding operators (:=, :=*, and :*=).
  */
abstract class TopModule(val nEdges: Int)(implicit p: Parameters = Parameters.empty) extends LazyModule {

  val sinkA = LazyModule(new InterruptsRecv()(new Config((_, _, _) => { case NEdgeSink => nEdges })))
  val sinkB = LazyModule(new InterruptsRecv()(new Config((_, _, _) => { case NEdgeSink => 1 })))
  val src   = LazyModule(new InterruptsGen()(new Config((_, _, _) => { case NEdgeSource => nEdges + 1 })))

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
    * edges drawn is determined by number of outward-edges missing in the RHS-node in the partially elaborated diplomatic graph.
    */
  sinkA.node :=* src.node
}

trait WithBindStar { this: TopModule =>
  /** Draw edges using :*= operator. BIND_STAR operator w.r.t LHS-node. BIND_QUERY operator w.r.t RHS-node. Number of
    * edges drawn is determined by number of inward-edges missing in the LHS-node in the partially elaborated diplomatic graph.
    */
  sinkA.node :*= src.node
}

trait WithBindFlex { this: TopModule =>
  val myAdapter = IntNameNode("myAdapter") 
  /** IntNameNode is an Identity node in which the number of inward and outward edges are same.
    * 
    */ 
  sinkA.node :*=* myAdapter :*= src.node
  //sinkA.node :*=* myAdapter :*= src.node
  //sinkA.node :=* myAdapter :*=* src.node
  //sinkA.node :*= myAdapter :*=* src.node
}


class Method1(n: Int) extends TopModule(n) with WithBindOnce
class Method2(n: Int) extends TopModule(n) with WithBindQuery
class Method3(n: Int) extends TopModule(n) with WithBindStar
class Method4(n: Int) extends TopModule(n) with WithBindFlex

class TopModuleImp(outer: TopModule) extends LazyModuleImp(outer) {
  println("Module implementation started\n!")
  val n_in = outer.src.node.out.map(x => x._2.source.num).fold(0)(_ + _)
  val sink_ports = outer.sinkA.node.in ++ outer.sinkB.node.in
  val n_out = sink_ports.map(x => x._2.source.num).fold(0)(_ + _) 
  val io = IO(new Bundle {
    val in  = Input(Vec(n_in, Bool()))
    val out = Output(Vec(n_out, Bool()))
  })

  io.out                        := (outer.sinkA.module.io.interrupt ++ outer.sinkB.module.io.interrupt)
  outer.src.module.io.interrupt := io.in
}
