ThisBuild / organization := "org.kapunga"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.1"

ThisBuild / scalacOptions ++= List(
  "-Ykind-projector",
  "-Xfatal-warnings")

import scala.scalanative.build.*

lazy val ttyTest = (project in file("tty"))
  .enablePlugins(ScalaNativePlugin)
  .settings(
    name := "TTY Test",
    logLevel := Level.Info,
    libraryDependencies ++= Seq(
      "co.fs2"         %%% "fs2-core"            % "3.10-4b5f50b",
      "co.fs2"         %%% "fs2-io"              % "3.10-4b5f50b"
    ),
    resolvers += "s01-oss-sonatype-org-snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots",
    nativeConfig ~= { c =>
    c.withLTO(LTO.none) // thin
      .withMode(Mode.debug) // releaseFast
      .withGC(GC.immix) // commix
    }
  )

