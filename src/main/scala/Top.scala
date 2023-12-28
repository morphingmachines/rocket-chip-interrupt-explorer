package explorer

import circt.stage.ChiselStage
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.util.ElaborationArtefacts
//import org.chipsalliance.cde.config.{Config, Parameters}

import java.io._
import java.nio.file._

trait Toplevel {
  def topModule: chisel3.RawModule
  def topclass_name = topModule.getClass().getName().split("\\$").mkString(".")

  def generated_sv_dir = s"generated_sv_dir/${topclass_name}"

  /** For firtoolOpts run `firtool --help` There is an overlap between ChiselStage args and firtoolOpts.
    *
    * TODO: Passing "--Split-verilog" "--output-annotation-file" to firtool is not working.
    */

  lazy val chiselArgs   = Array("--full-stacktrace", "--target-dir", generated_sv_dir, "--split-verilog")
  lazy val firtroolArgs = Array("-dedup")

  def chisel2firrtl() = {
    val str_firrtl = ChiselStage.emitCHIRRTL(topModule, args = Array("--full-stacktrace"))
    Files.createDirectories(Paths.get("generated_sv_dir"))
    val pw = new PrintWriter(new File(s"${generated_sv_dir}.fir"))
    pw.write(str_firrtl)
    pw.close()
  }

  // Call this only after calling chisel2firrtl()
  def firrtl2sv() =
    os.proc(
      "firtool",
      s"${generated_sv_dir}.fir",
      "--disable-annotation-unknown",
      "--split-verilog",
      s"-o=${generated_sv_dir}",
      s"--output-annotation-file=${generated_sv_dir}/${topclass_name}.anno.json",
    ).call() // check additional options with "firtool --help"

}

trait LazyToplevel extends Toplevel {
  def lazyTop: LazyModule
  override def topModule     = lazyTop.module
  override def topclass_name = lazyTop.getClass().getName().split("\\$").mkString(".")

  def genDiplomacyGraph() = {
    ElaborationArtefacts.add("graphml", lazyTop.graphML)
    ElaborationArtefacts.files.foreach {
      case ("graphml", graphML) =>
        val fw = new FileWriter(new File(s"${generated_sv_dir}", s"${lazyTop.className}.graphml"))
        fw.write(graphML())
        fw.close()
      case _ =>
    }
  }

  def showModuleComposition(gen: => LazyModule) = {
    println("List of Diplomatic Nodes (Ports)")
    gen.getNodes.map(x => println("Class Type: " + x.getClass.getName() + "| Instance (node) name: " + x.name))
    println("")
    println("List of Sub Modules")
    gen.getChildren.map(x => println("Class Type: " + x.getClass.getName() + "| Instance name:" + x.name))
  }

}

/** To run from a terminal shell
  * {{{
  * mill explorer.runMain explorer.interruptsMain
  * }}}
  */
object interruptsMain extends App with LazyToplevel {

  val lazyTop = args(0) match {
    case "example1" => LazyModule(new example1.TopModule)
    case "example2_1" => LazyModule(new example2.Method1(3))
    case "example2_2" => LazyModule(new example2.Method2(4))
    case "example2_3" => LazyModule(new example2.Method3(1))
    case "example2_4" => LazyModule(new example2.Method4(3))
    case "example3" => LazyModule(new example3.TopModule)
    case _ => throw new Exception("Unknown Module Name")
  }

  showModuleComposition(lazyTop)
  chisel2firrtl()
  firrtl2sv()
  genDiplomacyGraph()

}
