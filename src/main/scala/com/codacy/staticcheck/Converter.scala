package com.codacy.staticcheck

import java.nio.file.Path

import com.codacy.analysis.core.model.IssuesAnalysis.FileResults
import com.codacy.analysis.core.model.{IssuesAnalysis, ToolResults}
import com.codacy.analysis.core.serializer.IssuesReportSerializer

object Converter {

  def convert(lines: Seq[String], relativizeTo: Path): String = {
    val parsed = StaticcheckReportParser.parse(lines, relativizeTo)

    val grouped = parsed
      .map(Prefixer.withPrefix)
      .groupBy(_.filename)
      .view
      .map { case (path, res) => FileResults(path, res.to(Set)) }
      .to(Set)

    val toolResults = ToolResults("staticcheck", IssuesAnalysis.Success(grouped))
    IssuesReportSerializer.toJsonString(Set(toolResults))
  }

}
