package codacy

import codacy.staticcheck.Staticcheck
import com.codacy.tools.scala.seed.DockerEngine

object Engine extends DockerEngine(Staticcheck)()
