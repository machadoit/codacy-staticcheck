package com.codacy.staticcheck

import java.nio.file.{Path, Paths}

import com.codacy.analysis.core.model.Issue.Message
import com.codacy.analysis.core.model.{Issue, LineLocation}
import com.codacy.plugins.api.results.Pattern
import com.codacy.plugins.api.results.Result.Level
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StaticcheckReportParserSpecs extends AnyWordSpec with Matchers {
  val pwd: Path = Paths.get("/src")

  "ReportParser::parse" should {
    "parse simple result" in {
      val lines = Seq(
        """{"code":"S1034","severity":"error","location":{"file":"/src/configuration.go","line":55,"column":9},"end":{"file":"/Users/lorenzo/codacy/github/codacy-gorevive/configuration.go","line":55,"column":21},"message":"assigning the result of this type assertion to a variable (switch value := value.(type)) could eliminate type assertions in switch cases","related":[{"location":{"file":"/Users/lorenzo/codacy/github/codacy-gorevive/configuration.go","line":58,"column":21},"end":{"file":"/Users/lorenzo/codacy/github/codacy-gorevive/configuration.go","line":58,"column":37},"message":"could eliminate this type assertion"}]}""",
        """{"code":"S1009","severity":"error","location":{"file":"/src/configuration.go","line":90,"column":5},"end":{"file":"/Users/lorenzo/codacy/github/codacy-gorevive/configuration.go","line":90,"column":50},"message":"should omit nil check; len() for nil slices is defined as zero"}""",
        "",
      )

      val result = StaticcheckReportParser.parse(lines, relativizeTo = pwd)
      println(result)
      val expected = Seq(
        Issue(
          Pattern.Id("S1034"),
          Paths.get("configuration.go"),
          Message(
            "assigning the result of this type assertion to a variable (switch value := value.(type)) could eliminate type assertions in switch cases"
          ),
          Level.Err,
          Some(Pattern.Category.CodeStyle),
          LineLocation(55)
        ),
        Issue(
          Pattern.Id("S1009"),
          Paths.get("configuration.go"),
          Message("should omit nil check; len() for nil slices is defined as zero"),
          Level.Err,
          Some(Pattern.Category.CodeStyle),
          LineLocation(90)
        )
      )
      result should be(expected)
    }
  }

}
