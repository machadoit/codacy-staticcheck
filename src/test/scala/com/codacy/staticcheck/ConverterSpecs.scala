package com.codacy.staticcheck

import java.nio.file.Paths

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConverterSpecs extends AnyWordSpec with Matchers {

  val pwd = "/src"

  "Converter::convert" should {
    "convert output correctly" in {
      val lines =
        s"""{"code":"S1034","severity":"error","location":{"file":"$pwd/configuration.go","line":55,"column":9},"end":{"file":"$pwd/configuration.go","line":55,"column":21},"message":"assigning the result of this type assertion to a variable (switch value := value.(type)) could eliminate type assertions in switch cases","related":[{"location":{"file":"/Users/lorenzo/codacy/github/codacy-gorevive/configuration.go","line":58,"column":21},"end":{"file":"$pwd/configuration.go","line":58,"column":37},"message":"could eliminate this type assertion"}]}
           |{"code":"S1009","severity":"error","location":{"file":"$pwd/configuration.go","line":90,"column":5},"end":{"file":"$pwd/configuration.go","line":90,"column":50},"message":"should omit nil check; len() for nil slices is defined as zero"}
           |""".stripMargin.linesIterator.toSeq

      val expected =
        """[{"tool":"staticcheck","issues":{"Success":{"results":[{"filename":"configuration.go","results":[{"Issue":{"patternId":{"value":"Staticcheck_S1034"},"filename":"configuration.go","message":{"text":"assigning the result of this type assertion to a variable (switch value := value.(type)) could eliminate type assertions in switch cases"},"level":"Error","category":"CodeStyle","location":{"LineLocation":{"line":55}}}},{"Issue":{"patternId":{"value":"Staticcheck_S1009"},"filename":"configuration.go","message":{"text":"should omit nil check; len() for nil slices is defined as zero"},"level":"Error","category":"CodeStyle","location":{"LineLocation":{"line":90}}}}]}]}}}]"""

      Converter.convert(lines, relativizeTo = Paths.get("/src")) should be(expected)
    }
  }

}
