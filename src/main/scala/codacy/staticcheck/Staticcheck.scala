package codacy.staticcheck

import com.codacy.plugins.api.{Options, Source}
import com.codacy.plugins.api.results.Result.Issue
import com.codacy.plugins.api.results.{Parameter, Pattern, Result, Tool}
import com.codacy.tools.scala.seed.utils.CommandRunner
import com.codacy.tools.scala.seed.utils.ToolHelper._
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.util.{Failure, Properties, Success, Try}

object Staticcheck extends Tool {

  override def apply(source: Source.Directory,
                     configuration: Option[List[Pattern.Definition]],
                     files: Option[Set[Source.File]],
                     options: Map[Options.Key, Options.Value])(implicit specification: Tool.Specification): Try[List[Result]] = {

    val patternsToLintOpt: Option[List[Pattern.Definition]] = configuration.withDefaultParameters

    patternsToLintOpt.fold[Try[List[Result]]](Success(List.empty[Result])) {
      patternsToLint =>
        val patternsToLintSet: Set[Pattern.Id] =
          patternsToLint.map(_.patternId)(
            collection
              .breakOut[List[Pattern.Definition], Pattern.Id, Set[Pattern.Id]])

        val patternsToIgnore: Set[Pattern.Id] = specification.patterns.map(
          _.patternId) diff patternsToLintSet

        val ignorePatternsCmd: Seq[String] = if (patternsToIgnore.nonEmpty) {
          Seq("-ignore", s"*:${patternsToIgnore.map(_.value).mkString(",")}")
        } else {
          Seq.empty[String]
        }

        val command = List("/opt/docker/go/bin/staticcheck", "-f", "json") ++ ignorePatternsCmd

        CommandRunner.exec(command, Some(new java.io.File(source.path))) match {
          case Right(resultFromTool) if resultFromTool.stderr.isEmpty =>
            parseToolResult(resultFromTool.stdout).map(_.filter(result =>
              resultFilter(result, patternsToLintSet, files))) match {
              case s @ Success(_) => s
              case Failure(e) =>
                val msg =
                  s"""
                     |${e.getStackTrace.mkString(System.lineSeparator)}
                     |Staticcheck exited with code ${resultFromTool.exitCode}
                     |message: ${e.getMessage}
                     |stdout: ${resultFromTool.stdout.mkString(
                    Properties.lineSeparator)}
                     |stderr: ${resultFromTool.stderr.mkString(
                    Properties.lineSeparator)}
                """.stripMargin
                Failure(new Exception(msg))
            }

          case Right(resultFromTool) =>
            val msg =
              s"""
                 |Staticcheck exited with code ${resultFromTool.exitCode} with output on the stderr
                 |stdout: ${resultFromTool.stdout.mkString(
                Properties.lineSeparator)}
                 |stderr: ${resultFromTool.stderr.mkString(
                Properties.lineSeparator)}
                """.stripMargin

            Failure(new Exception(msg))

          case Left(e) =>
            Failure(e)
        }
    }
  }

  private def parseToolResult(
      resultsFromTool: List[String]): Try[List[Result]] = {
    Try {
      val results: List[Try[Result]] = resultsFromTool.map { resultRaw =>
        Json.parse(resultRaw).validate[ToolNativeResult] match {
          case JsSuccess(nativeResult, _) => Success(toResult(nativeResult))
          case JsError(errors) =>
            val msg =
              s"""Error parsing native results to docker tool results:
                 |${errors.mkString(System.lineSeparator)}""".stripMargin
            Failure(new Exception(msg))
        }
      }

      val maybeFailure: Option[Try[Result]] = results.find(_.isFailure)

      maybeFailure.fold[Try[List[Result]]](Success(results.flatMap(_.toOption))) {
        failure =>
          failure.map(List(_))
      }
    }.flatten
  }

  private def resultFilter(result: Result,
                           patternsToLintSet: Set[Pattern.Id],
                           filesOpt: Option[Set[Source.File]]): Boolean = {
    def patternFilter(patternId: Pattern.Id): Boolean = {
      patternsToLintSet.contains(patternId)
    }

    def fileFilter(file: Source.File): Boolean = {
      filesOpt.exists(_.contains(file))
    }

    result match {
      case Issue(file, _, patternId, _) =>
        patternFilter(patternId) && fileFilter(file)
      case _ =>
        true
    }
  }

  private def toResult(nativeResult: ToolNativeResult): Result = {
    Result.Issue(Source.File(nativeResult.location.file),
                 Result.Message(nativeResult.message),
                 Pattern.Id(nativeResult.code),
                 Source.Line(nativeResult.location.line))
  }
}
