lazy val core = project.in(file("."))
    .settings(commonSettings)
    .settings(
      name := "$name$"
    )

lazy val V = new {
  val cats = "$catsV$"
  val kittens = "$kittensV$"
  val catsEffect = "$catsEffectV$"
  val mouse = "$mouseV$"
  val shapeless = "$shapelessV$"
  val fs2 = "$fs2V$"
  val circe = "$circeV$"
  val droste = "$drosteV$"
  val specs2 = "$specs2V$"
  val discipline = "$disciplineV$"
}


lazy val contributors = Seq(
  "$contributorUsername$" -> "$contributorName$"
)

// check for library updates whenever the project is [re]load
onLoad in Global := { s =>
  "dependencyUpdates" :: s
}

// General Settings
lazy val commonSettings = Seq(
  organization := "$organization$",

  scalaVersion := "$scala_version$",
  crossScalaVersions := Seq(scalaVersion.value, "$other_scala_version$"),
  scalafmtOnCompile in ThisBuild := true,

  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.7" cross CrossVersion.binary),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4"),
  libraryDependencies ++= Seq(
    "org.typelevel"               %% "cats-core"                  % V.cats,

    "org.typelevel"               %% "kittens"                    % V.kittens,
    "org.typelevel"               %% "alleycats-core"             % V.cats,
    "org.typelevel"               %% "mouse"                      % V.mouse,

    "org.typelevel"               %% "cats-effect"                % V.catsEffect,

    "com.chuusai"                 %% "shapeless"                  % V.shapeless,

    "co.fs2"                      %% "fs2-core"                   % V.fs2,
    "co.fs2"                      %% "fs2-io"                     % V.fs2,

    "io.circe"                    %% "circe-core"                 % V.circe,
    "io.circe"                    %% "circe-generic"              % V.circe,
    "io.circe"                    %% "circe-parser"               % V.circe,

    "org.specs2"                  %% "specs2-core"                % V.specs2       % Test,
    "org.specs2"                  %% "specs2-scalacheck"          % V.specs2       % Test,
    "org.typelevel"               %% "discipline"                 % V.discipline   % Test,
  )
)

lazy val releaseSettings = {
  import ReleaseTransformations._
  Seq(
    releaseCrossBuild := true,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      // For non cross-build projects, use releaseStepCommand("publishSigned")
      releaseStepCommandAndRemaining("+publishSigned"),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    ),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    credentials ++= (
      for {
        username <- Option(System.getenv().get("SONATYPE_USERNAME"))
        password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
      } yield
        Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          username,
          password
        )
    ).toSeq,
    publishArtifact in Test := false,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/$contributorUsername$/$name$"),
        "git@github.com:$contributorUsername$/$name$.git"
      )
    ),
    homepage := Some(url("https://github.com/$contributorUsername$/$name$")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishMavenStyle := true,
    pomIncludeRepository := { _ =>
      false
    },
    pomExtra := {
      <developers>
        {for ((username, name) <- contributors) yield
        <developer>
          <id>{username}</id>
          <name>{name}</name>
          <url>http://github.com/{username}</url>
        </developer>
        }
      </developers>
    }
  )
}

lazy val mimaSettings = {
  import sbtrelease.Version

  def semverBinCompatVersions(major: Int, minor: Int, patch: Int): Set[(Int, Int, Int)] = {
    val majorVersions: List[Int] = List(major)
    val minorVersions : List[Int] = 
      if (major >= 1) Range(0, minor).inclusive.toList
      else List(minor)
    def patchVersions(currentMinVersion: Int): List[Int] = 
      if (minor == 0 && patch == 0) List.empty[Int]
      else if (currentMinVersion != minor) List(0)
      else Range(0, patch - 1).inclusive.toList

    val versions = for {
      maj <- majorVersions
      min <- minorVersions
      pat <- patchVersions(min)
    } yield (maj, min, pat)
    versions.toSet
  }

  def mimaVersions(version: String): Set[String] = {
    Version(version) match {
      case Some(Version(major, Seq(minor, patch), _)) =>
        semverBinCompatVersions(major.toInt, minor.toInt, patch.toInt)
          .map{case (maj, min, pat) => maj.toString + "." + min.toString + "." + pat.toString}
      case _ =>
        Set.empty[String]
    }
  }
  // Safety Net For Exclusions
  lazy val excludedVersions: Set[String] = Set()

  // Safety Net for Inclusions
  lazy val extraVersions: Set[String] = Set()

  Seq(
    mimaFailOnProblem := mimaVersions(version.value).toList.headOption.isDefined,
    mimaPreviousArtifacts := (mimaVersions(version.value) ++ extraVersions)
      .filterNot(excludedVersions.contains(_))
      .map{v => 
        val moduleN = moduleName.value + "_" + scalaBinaryVersion.value.toString
        organization.value % moduleN % v
      },
    mimaBinaryIssueFilters ++= {
      import com.typesafe.tools.mima.core._
      import com.typesafe.tools.mima.core.ProblemFilters._
      Seq()
    }
  )
}

lazy val skipOnPublishSettings = Seq(
  skip in publish := true,
  publish := (()),
  publishLocal := (()),
  publishArtifact := false,
  publishTo := None
)
