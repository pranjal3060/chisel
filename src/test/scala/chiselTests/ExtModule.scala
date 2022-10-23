// SPDX-License-Identifier: Apache-2.0

package chiselTests

import chisel3._
import chisel3.experimental._
import chisel3.stage.ChiselStage
import chisel3.testers.{BasicTester, TesterDriver}

// Avoid collisions with regular BlackBox tests by putting ExtModule blackboxes
// in their own scope.
package ExtModule {

  import chisel3.experimental.ExtModule

  class BlackBoxInverter extends ExtModule {
    val in = IO(Input(Bool()))
    val out = IO(Output(Bool()))
  }

  class BlackBoxPassthrough extends ExtModule {
    val in = IO(Input(Bool()))
    val out = IO(Output(Bool()))
  }
}

class ExtModuleTester extends BasicTester {
  val blackBoxPos = Module(new ExtModule.BlackBoxInverter)
  val blackBoxNeg = Module(new ExtModule.BlackBoxInverter)

  blackBoxPos.in := 1.U
  blackBoxNeg.in := 0.U

  assert(blackBoxNeg.out === 1.U)
  assert(blackBoxPos.out === 0.U)
  stop()
}

/** Instantiate multiple BlackBoxes with similar interfaces but different
  * functionality. Used to detect failures in BlackBox naming and module
  * deduplication.
  */

class MultiExtModuleTester extends BasicTester {
  val blackBoxInvPos = Module(new ExtModule.BlackBoxInverter)
  val blackBoxInvNeg = Module(new ExtModule.BlackBoxInverter)
  val blackBoxPassPos = Module(new ExtModule.BlackBoxPassthrough)
  val blackBoxPassNeg = Module(new ExtModule.BlackBoxPassthrough)

  blackBoxInvPos.in := 1.U
  blackBoxInvNeg.in := 0.U
  blackBoxPassPos.in := 1.U
  blackBoxPassNeg.in := 0.U

  assert(blackBoxInvNeg.out === 1.U)
  assert(blackBoxInvPos.out === 0.U)
  assert(blackBoxPassNeg.out === 0.U)
  assert(blackBoxPassPos.out === 1.U)
  stop()
}

class ExtModuleInvalidatedTester extends MultiIOModule {
  val in = IO(Input(UInt(8.W)))
  val out = IO(Output(UInt(8.W)))
  val inst = Module(new ExtModule {
    val in = IO(Input(UInt(8.W)))
    val out = IO(Output(UInt(8.W)))
  })
  inst.in := in
  out := inst.out
}

class ExtModuleSpec extends ChiselFlatSpec {
  "A ExtModule inverter" should "work" in {
    assertTesterPasses({ new ExtModuleTester },
        Seq("/chisel3/BlackBoxTest.v"), TesterDriver.verilatorOnly)
  }
  "Multiple ExtModules" should "work" in {
    assertTesterPasses({ new MultiExtModuleTester },
        Seq("/chisel3/BlackBoxTest.v"), TesterDriver.verilatorOnly)
  }
  "DataMirror.modulePorts" should "work with ExtModule" in {
    ChiselStage.elaborate(new Module {
      val io = IO(new Bundle { })
      val m = Module(new ExtModule.BlackBoxPassthrough)
      assert(DataMirror.modulePorts(m) == Seq(
          "in" -> m.in, "out" -> m.out))
    })
  }

  behavior.of("ExtModule")

  it should "not have invalidated ports in a chisel3._ context" in {
    val chirrtl = ChiselStage.emitChirrtl(new ExtModuleInvalidatedTester)
    chirrtl shouldNot include("inst.in is invalid")
    chirrtl shouldNot include("inst.out is invalid")
  }
}
