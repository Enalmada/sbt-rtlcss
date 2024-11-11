lazy val `sbt-rtlcss` = project in file(".")

enablePlugins(SbtWebBase)

organization := "com.github.enalmada"
name := "sbt-rtlcss"
description := "sbt-web plugin for creating rtl versions of css"

addSbtJsEngine("1.3.9")

libraryDependencies ++= Seq(
  "org.webjars.npm" % "rtlcss" % "4.1.1",
  "io.monix" %% "monix" % "3.4.1"
)

// Maven settings
publishMavenStyle := true
startYear := Some(2018)
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
Test / publishArtifact := false
pomIncludeRepository := { _ => false }
Global / pomExtra := {
  <scm>
    <connection>scm:git:git@github.com:Enalmada/sbt-rtlcss.git</connection>
    <developerConnection>scm:git:git@github.com:Enalmada/sbt-rtlcss.git</developerConnection>
    <url>https://github.com/enalmada</url>
  </scm>
    <developers>
      <developer>
        <id>enalmada</id>
        <name>Adam Lane</name>
        <url>https://github.com/enalmada</url>
      </developer>
    </developers>
}
credentials ++= {
  val pathToCredentials = Path.userHome / ".sbt" / "sonatype.credentials"
  if (pathToCredentials.exists()) Seq(Credentials(pathToCredentials)) else Nil
}

// Sbt-sonatype fix
Compile / doc / sources := Seq()


