
lazy val root = (project in file(".")).enablePlugins(SbtWeb)

//JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

libraryDependencies += "org.webjars" % "bootstrap" % "3.0.2"

scalaVersion := "2.12.4"

pipelineStages := Seq(rtlcss)
