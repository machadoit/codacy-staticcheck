package codacy.staticcheck

import play.api.libs.json.Json

case class ToolNativeResult(code: String, location: ToolNativeLocation, message: String)

case class ToolNativeLocation(file: String, line: Int)

object ToolNativeLocation {
  implicit lazy val fmt = Json.format[ToolNativeLocation]
}

object ToolNativeResult {
  implicit lazy val fmt = Json.format[ToolNativeResult]
}

