package codacy

import codacy.dockerApi.DockerEngine
import codacy.staticcheck.Staticcheck

object Engine extends DockerEngine(Staticcheck)
