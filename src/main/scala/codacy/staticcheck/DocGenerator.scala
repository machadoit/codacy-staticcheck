package codacy.staticcheck

import codacy.helpers.ResourceHelper
import play.api.libs.json.{JsArray, Json}

case class PatternInfo(patternId: String, title: String)

object DocGenerator {
  def main(args: Array[String]): Unit = {
    args.headOption.fold {
      throw new Exception("Version parameter is required (ex: 2017.2.2)")
    } { version =>
      val rules: Seq[PatternInfo] = generateRules(s"raw-docs/$version")

      val repoRoot = new java.io.File(".")
      val docsRoot = new java.io.File(repoRoot, "src/main/resources/docs")
      val patternsFile = new java.io.File(docsRoot, "patterns.json")
      val descriptionsRoot = new java.io.File(docsRoot, "description")
      val descriptionsFile = new java.io.File(descriptionsRoot, "description.json")

      val patterns = Json.prettyPrint(Json.obj("name" -> "Staticcheck", "version" -> version, "patterns" -> Json.parse(Json.toJson(generatePatterns(rules)).toString).as[JsArray]))

      val descriptions = Json.prettyPrint(Json.parse(Json.toJson(generateDescriptions(rules, descriptionsRoot)).toString).as[JsArray])

      ResourceHelper.writeFile(patternsFile.toPath, patterns)
      ResourceHelper.writeFile(descriptionsFile.toPath, descriptions)
    }
  }

  private def generatePatterns(rules: Seq[PatternInfo]): JsArray = {
    val codacyPatterns = rules.collect { case rule =>
      val (category, level) = rule.patternId match {
        case patternId if patternId.startsWith("SA1") =>
          ("CodeStyle", "Info")
        case patternId if patternId.startsWith("SA2") =>
          ("ErrorProne", "Error")
        case patternId if patternId.startsWith("SA3") =>
          ("ErrorProne", "Warning")
        case patternId if patternId.startsWith("SA4") =>
          ("UnusedCode", "Info")
        case patternId if patternId.startsWith("SA5") =>
          ("CodeStyle", "Warning")
        case patternId if patternId.startsWith("SA6") =>
          ("Performance", "Warning")
        case patternId if patternId.startsWith("SA9") =>
          ("CodeStyle", "Error")
        case _ =>
          ("CodeStyle", "Info")
      }

      Json.obj(
        "patternId" -> rule.patternId,
        "level" -> level,
        "category" -> category
      )
    }
    Json.parse(Json.toJson(codacyPatterns).toString).as[JsArray]
  }

  private def generateDescriptions(rules: Seq[PatternInfo], descriptionsRoot: java.io.File): JsArray = {
    val codacyPatternsDescs = rules.collect { case rule =>

      Json.obj(
        "patternId" -> rule.patternId,
        "title" -> Json.toJsFieldJsValueWrapper(Option(truncateText(rule, 250)).filter(_.nonEmpty).getOrElse(rule.patternId)),
        "timeToFix" -> 5
      ) ++
        Option(truncateText(rule, 495)).filter(_.nonEmpty)
          .fold(Json.obj())(desc => Json.obj("description" -> desc))
    }

    Json.parse(Json.toJson(codacyPatternsDescs).toString).as[JsArray]
  }

  private def truncateText(rule: PatternInfo, maxCharacters: Int): String = {
    val description = rule.title
    if (description.length > maxCharacters) {
      description.take(maxCharacters).split("\\.").dropRight(1).mkString(".") + "."
    } else {
      description
    }
  }

  private def generateRules(rawDocsPath: String): Seq[PatternInfo] = {
    ResourceHelper.listResourceDirectory(rawDocsPath).get.map { patternId =>
      val patternTitle = ResourceHelper.getResourceContent(s"$rawDocsPath/$patternId").get.head
      PatternInfo(patternId, patternTitle)
    }
  }

}