package codacy.staticcheck

import codacy.docker.api
import codacy.docker.api.Result.Issue
import codacy.docker.api._
import codacy.docker.api.utils.ToolHelper
import codacy.dockerApi.utils.CommandRunner
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.util.{Failure, Properties, Success, Try}

object Staticcheck extends Tool {

  override def apply(source: api.Source.Directory,
                     configuration: Option[List[Pattern.Definition]],
                     filesOpt: Option[Set[api.Source.File]])(
                      implicit specification: Tool.Specification): Try[List[Result]] = {

    val patternsToLintOpt: Option[List[Pattern.Definition]] =
      ToolHelper.patternsToLint(configuration)

    patternsToLintOpt.fold[Try[List[Result]]](Success(List.empty[Result])) { patternsToLint =>

      val patternsToLintSet: Set[Pattern.Id] = patternsToLint.map(_.patternId)(collection.breakOut[List[Pattern.Definition], Pattern.Id, Set[Pattern.Id]])

      val patternsToIgnore: Set[Pattern.Id] = specification.patterns.map(_.patternId) diff patternsToLintSet

      val ignorePatternsCmd: Seq[String] = if (patternsToIgnore.nonEmpty) {
        Seq("-ignore", s"*:${patternsToIgnore.map(_.value).mkString(",")}")
      } else {
        Seq.empty[String]
      }

      val command = List("/opt/docker/go/bin/staticcheck", "-f", "json") ++ ignorePatternsCmd

      CommandRunner.exec(command, Some(new java.io.File(source.path))) match {
        case Right(resultFromTool) =>
          withResultsFilter(patternsToLintSet, filesOpt)(parseToolResult(resultFromTool.stdout)) match {
            case s@Success(_) => s
            case Failure(e) =>
              val msg =
                s"""
                   |${e.getStackTrace.mkString(System.lineSeparator)}
                   |Staticcheck exited with code ${resultFromTool.exitCode}
                   |message: ${e.getMessage}
                   |stdout: ${
                  resultFromTool.stdout.mkString(
                    Properties.lineSeparator)
                }
                   |stderr: ${
                  resultFromTool.stderr.mkString(
                    Properties.lineSeparator)
                }
                """.stripMargin
              Failure(new Exception(msg))
          }

        case Left(e) =>
          Failure(e)
      }

    }
  }

  private def parseToolResult(resultsFromTool: List[String]): Try[List[Result]] = {
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

    maybeFailure.fold[Try[List[Result]]](Success(results.flatMap(_.toOption))) { failure =>
      failure.map(List(_))
    }
  }

  private def withResultsFilter(patternsToLint: Set[Pattern.Id], filesOpt: Option[Set[api.Source.File]])
                               (body: => Try[List[Result]]): Try[List[Result]] = {
    filesOpt.fold(body) { filesAllowed =>
      body.map { results =>
        results.flatMap(result => withResultFileFilter(result, filesAllowed))
      }
    }.map { results =>
      results.flatMap(result => withResultPatternFilter(result, patternsToLint))
    }
  }

  private def withResultFileFilter(result: Result, filesAllowed: Set[api.Source.File]): Option[Result] = {
    result match {
      case Issue(file, _, _, _) if !filesAllowed.exists(allowedFile => allowedFile.path == file.path) =>
        None
      case _ =>
        Some(result)
    }
  }

  private def withResultPatternFilter(result: Result, patternsToLint: Set[Pattern.Id]): Option[Result] = {
    result match {
      case Issue(_, _, patternId, _) if !patternsToLint.exists(allowedPattern => allowedPattern.value == patternId.value) =>
        None
      case _ =>
        Some(result)
    }
  }

  private def toResult(nativeResult: ToolNativeResult): Result = {
    Result.Issue(api.Source.File(nativeResult.location.file), Result.Message(nativeResult.message),
      Pattern.Id(nativeResult.code), api.Source.Line(nativeResult.location.line))
  }
}
