package explorer

//import org.chipsalliance.cde.config.{Config, Parameters}

import org.chipsalliance.diplomacy.lazymodule.LazyModule

/** To run from a terminal shell
  * {{{
  * mill explorer.runMain explorer.interruptsMain
  * }}}
  */
object interruptsMain extends App with emitrtl.LazyToplevel {

  val lazyTop = args(0) match {
    case "example1"   => LazyModule(new example1.TopModule)
    case "example2_1" => LazyModule(new example2.Method1(3))
    case "example2_2" => LazyModule(new example2.Method2(3))
    case "example2_3" => LazyModule(new example2.Method3(1))
    case "example2_4" => LazyModule(new example2.Method4(3))
    case "example3"   => LazyModule(new example3.TopModule)
    case "example4"   => LazyModule(new example4.TopModule)
    case _            => throw new Exception("Unknown Module Name")
  }

  showModuleComposition(lazyTop)
  chisel2firrtl()
  firrtl2sv()
  genDiplomacyGraph()

}
