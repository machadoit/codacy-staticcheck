package com.codacy.staticcheck

import java.nio.file.Paths

object Main {
  val toolName = "staticcheck"

  def main(args: Array[String]): Unit = {
    val stdin = scala.io.Source.fromInputStream(System.in)
    val lines = stdin.getLines().to(LazyList)
    val pwd = Paths.get(System.getProperty("user.dir"))
    val jsonString = Converter.convert(lines, relativizeTo = pwd)
    println(jsonString)
  }

}
