name := "saio"
organization := "io.github.idata-shopee"
version := "0.0.1"
scalaVersion := "2.12.4"

useGpg := true 
parallelExecution in Test := true

publishTo := sonatypePublishTo.value

libraryDependencies ++= Seq(
  // test suite
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)
