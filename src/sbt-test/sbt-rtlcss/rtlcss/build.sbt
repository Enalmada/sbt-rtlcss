lazy val root = (project in file(".")).enablePlugins(SbtWeb)

libraryDependencies += "org.webjars" % "bootstrap" % "3.3.7"

scalaVersion := "2.12.4"

pipelineStages := Seq(rtlcss)

includeFilter in rtlcss := GlobFilter("a.css")

//excludeFilter in rtlcss := GlobFilter("bad.css") || GlobFilter("*.rtl.css")


val checkMapFileContents = taskKey[Unit]("check that map contents are correct")

checkMapFileContents := {
  val contents = IO.read(file("target/web/stage/stylesheets/a.rtl.css"))
  val r = """.example {
            |	padding:5px 20px 15px 11px;
            |}""".r
  if (r.findAllIn(contents).isEmpty) {
    sys.error(s"Unexpected contents: $contents")
  }
}