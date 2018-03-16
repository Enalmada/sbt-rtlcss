
organization := "com.github.enalmada"
name := "sbt-rtlcss"
description := "sbt-web plugin for creating rtl versions of css"
addSbtJsEngine("1.2.2")
libraryDependencies ++= Seq(
  "org.webjars.npm" % "rtlcss" % "2.2.1",
  "io.monix" %% "monix" % "2.3.0"
)
resolvers += Resolver.url("bintray-sbt-plugins", url("https://dl.bintray.com/sbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
scalaVersion := "2.12.4"
//scriptedBufferLog := false

//addSbtPlugin("com.typesafe.sbt" %% "sbt-web" % "1.4.3")

//lazy val `sbt-rtlcss` = (project in file(".")).enablePlugins(SbtWeb)

//*******************************
// Maven settings
//*******************************

publishMavenStyle := true

startYear := Some(2018)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra in Global := {
    <scm>
      <connection>git@github.com:Enalmada/sbt-rtlcss.git</connection>
      <developerConnection>git@github.com:Enalmada/sbt-rtlcss.git</developerConnection>
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

credentials += Credentials(Path.userHome / ".sbt" / "sonatype.credentials")

// https://github.com/xerial/sbt-sonatype/issues/30
sources in (Compile, doc) := Seq()