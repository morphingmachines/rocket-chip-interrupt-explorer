// import Mill dependency
import mill._
import mill.define.Sources
import mill.modules.Util
import mill.scalalib.TestModule.ScalaTest
import scalalib._
import publish._
// support BSP
import mill.bsp._

import scalafmt._
import $ivy.`com.goyeau::mill-scalafix::0.3.1`
import com.goyeau.mill.scalafix.ScalafixModule

import $file.^.playground.{builddefs => playground_build}
import $file.^.playground.dependencies.cde.{build => cde_build}
import $file.^.playground.dependencies.`rocket-chip`.{common => rocketchip_common}
import $file.^.playground.dependencies.diplomacy.{common => diplomacy_common}

object ivys {
  val cv = playground_build.ivys.cv
}

object macros extends rocketchip_common.MacrosModule with SbtModule {
  override def millSourcePath = os.pwd / os.up / "playground" / "dependencies" / "rocket-chip" / "macros"
  def scalaVersion: T[String] = T(playground_build.ivys.sv)
  def scalaReflectIvy = playground_build.ivys.scalaReflect
}

object mycde extends cde_build.CDE with PublishModule {
  override def millSourcePath = os.pwd / os.up / "playground" / "dependencies" / "cde" / "cde"
  def scalaVersion: T[String] = T(playground_build.ivys.sv)
}

object mydiplomacy extends diplomacy_common.DiplomacyModule with playground_build.CommonModule {
  override def millSourcePath = os.pwd / os.up / "playground" / "dependencies" / "diplomacy" / "diplomacy"
  override def scalaVersion   = playground_build.ivys.sv
  def chiselModule            = None
  def chiselPluginJar         = None
  def chiselIvy               = Some(playground_build.ivys.chiselCrossVersions(ivys.cv)._1)
  def chiselPluginIvy         = Some(playground_build.ivys.chiselCrossVersions(ivys.cv)._2)
  def sourcecodeIvy           = playground_build.ivys.sourcecode
  def cdeModule               = mycde
}

object myrocketchip extends rocketchip_common.RocketChipModule with SbtModule {

  override def millSourcePath = os.pwd / os.up / "playground" / "dependencies" / "rocket-chip"

  override def scalaVersion = playground_build.ivys.sv

  def chiselModule = None

  def chiselPluginJar = None

  def chiselIvy = Some(playground_build.ivys.chiselCrossVersions(ivys.cv)._1)

  def chiselPluginIvy = Some(playground_build.ivys.chiselCrossVersions(ivys.cv)._2)

  override def ivyDeps             = T(super.ivyDeps() ++ chiselIvy)
  override def scalacPluginIvyDeps = T(super.scalacPluginIvyDeps() ++ chiselPluginIvy)

  def macrosModule = macros

  def hardfloatModule: ScalaModule = myhardfloat

  def cdeModule: ScalaModule = mycde

  def diplomacyModule: ScalaModule = mydiplomacy

  def mainargsIvy = playground_build.ivys.mainargs

  def json4sJacksonIvy = playground_build.ivys.json4sJackson

}

// UCB
object myhardfloat extends ScalaModule with SbtModule with PublishModule {
  override def millSourcePath = os.pwd / os.up / "playground" / "dependencies" / "berkeley-hardfloat"
  def scalaVersion            = playground_build.ivys.sv

  def chiselIvy = Some(playground_build.ivys.chiselCrossVersions(ivys.cv)._1)

  def chiselPluginIvy = Some(playground_build.ivys.chiselCrossVersions(ivys.cv)._2)

  override def ivyDeps             = T(super.ivyDeps() ++ chiselIvy)
  override def scalacPluginIvyDeps = T(super.scalacPluginIvyDeps() ++ chiselPluginIvy)
  // remove test dep
  override def allSourceFiles = T(
    super.allSourceFiles().filterNot(_.path.last.contains("Tester")).filterNot(_.path.segments.contains("test")),
  )

  def publishVersion = de.tobiasroeser.mill.vcs.version.VcsVersion.vcsState().format()

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "edu.berkeley.cs",
    url = "http://chisel.eecs.berkeley.edu",
    licenses = Seq(License.`BSD-3-Clause`),
    versionControl = VersionControl.github("ucb-bar", "berkeley-hardfloat"),
    developers = Seq(
      Developer("jhauser-ucberkeley", "John Hauser", "https://www.colorado.edu/faculty/hauser/about/"),
      Developer("aswaterman", "Andrew Waterman", "https://aspire.eecs.berkeley.edu/author/waterman/"),
      Developer("yunsup", "Yunsup Lee", "https://aspire.eecs.berkeley.edu/author/yunsup/"),
    ),
  )
}

object inclusivecache extends playground_build.CommonModule {
  override def millSourcePath =
    os.pwd / os.up / "playground" / "dependencies" / "rocket-chip-inclusive-cache" / "design" / "craft" / "inclusivecache"
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip)
}

object testchip extends playground_build.CommonModule with SbtModule {
  override def millSourcePath = os.pwd / os.up / "playground" / "dependencies" / "testchipip"
  override def moduleDeps     = super.moduleDeps ++ Seq(myrocketchip, blocks)
}

object blocks extends playground_build.CommonModule with SbtModule {
  override def millSourcePath = os.pwd / os.up / "playground" / "dependencies" / "rocket-chip-blocks"
  override def moduleDeps     = super.moduleDeps ++ Seq(myrocketchip)
}

object shells extends playground_build.CommonModule with SbtModule {
  override def millSourcePath = os.pwd / os.up / "playground" / "dependencies" / "rocket-chip-fpga-shells"
  override def moduleDeps     = super.moduleDeps ++ Seq(myrocketchip)
}

object emitrtl extends playground_build.CommonModule with SbtModule {
  override def millSourcePath = os.pwd / "dependencies" / "emitrtl"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip)
}

trait ScalacOptions extends ScalaModule {
  override def scalacOptions = T {
    super.scalacOptions() ++ Seq(
      "-unchecked",
      "-deprecation",
      "-language:reflectiveCalls",
      "-feature",
      "-Xcheckinit",
      "-Xfatal-warnings",
      "-Ywarn-dead-code",
      "-Ywarn-unused",
    )
  }
}

object explorer
  extends playground_build.CommonModule
  with SbtModule
  with ScalafmtModule
  with ScalafixModule
  with ScalacOptions {
  override def millSourcePath = os.pwd

  override def moduleDeps = super.moduleDeps ++ Seq(mycde, mydiplomacy, myrocketchip, emitrtl)

  object test extends SbtModuleTests with ScalaTest with ScalafmtModule with ScalafixModule {
    override def ivyDeps = super.ivyDeps() ++ Agg(
      playground_build.ivys.scalatest,
      playground_build.ivys.oslib,
    )

  }
}