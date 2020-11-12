package com.codacy.staticcheck

import java.nio.file.{Path, Paths}

import com.codacy.analysis.core.model.Issue.Message
import com.codacy.analysis.core.model.{Issue, LineLocation}
import com.codacy.plugins.api.results.Pattern
import com.codacy.plugins.api.results.Result.Level

object StaticcheckReportParser {

  def parse(lines: Seq[String], relativizeTo: Path): Seq[Issue] = {

    lines.filter(_.nonEmpty).map { line =>
      val json = ujson.read(line)
      val rule = json("code").str
      val location = json("location")
      Issue(
        patternId = Pattern.Id(rule),
        filename = relativizeTo.relativize(Paths.get(location("file").str).toAbsolutePath()),
        message = Message(json("message").str),
        level = json("severity").str match {
          case "error" => Level.Err
          case "warning" => Level.Warn
          case _ => Level.Info
        },
        category = Some(Utils.ruleToCategoryAndLevel(rule)._1),
        location = LineLocation(location("line").num.toInt)
      )
    }
  }
}
