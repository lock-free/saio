sonatypeProfileName := "io.github.lock-free"

publishMavenStyle := true

licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("lock-free", "saio", "os.lock.free@gmail.com"))

homepage := Some(url("https://github.com/lock-free/saio"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/lock-free/saio"),
    "scm:git@github.com:lock-free/saio.git"
  )
)

developers := List(
  Developer(id="ddchen", name="ddchen", email="chenjunyuwork@gmail.com", url=url("http://lovekino.github.io/"))
)
