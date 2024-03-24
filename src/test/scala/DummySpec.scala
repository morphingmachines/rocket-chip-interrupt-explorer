package explorer

import chisel3._
import chiseltest._
import freechips.rocketchip.diplomacy.LazyModule
//import org.chipsalliance.cde.config.Config
import org.scalatest.freespec.AnyFreeSpec

/** This is a trivial example of how to run this Specification: From a terminal shell use:
  * {{{
  * mill adder.test.testOnly adder.DiplomaticAdder
  * }}}
  */
class DummySpec extends AnyFreeSpec with ChiselScalatestTester {

  "Dummy Test" in {
    test(
      LazyModule(
        new example1.TopModule,
      ).module,
    ).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        VerilatorBackendAnnotation, // Uncomment to use the Verilator backend
      ),
    ) { dut =>
      dut.io.in(0).poke(true.B)
      println(dut.io.out.peek())
      dut.clock.step(10)
    }
  }
}
