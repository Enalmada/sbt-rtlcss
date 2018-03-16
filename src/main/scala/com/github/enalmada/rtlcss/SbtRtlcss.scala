package com.github.enalmada.rtlcss

import com.github.enalmada.proguard.Sbt10Compat.SbtIoPath._
import com.typesafe.sbt.jse.{SbtJsEngine, SbtJsTask}
import com.typesafe.sbt.web.incremental._
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.{Compat, PathMapping, SbtWeb, incremental}
import monix.reactive.Observable
import sbt.Keys._
import sbt.{Task, _}

import scala.concurrent.Await

object Import {

  val rtlcss = TaskKey[Pipeline.Stage]("rtlcss", "Perform rtlcss optimization on the asset pipeline.")

  val rtlcssBuildDir = settingKey[File]("Where rtlcss will copy source files and write minified files to. Default: resourceManaged / build")
  val rtlcssComments = settingKey[Option[String]]("Specifies comments handling. Default: None")
  val rtlcssCompress = settingKey[Boolean]("Enables compression. Default: true")
  val rtlcssCompressOptions = settingKey[Seq[String]]("Options for compression such as hoist_vars, if_return etc. Default: Nil")
  val rtlcssDefine = settingKey[Option[String]]("Define globals. Default: None")
  val rtlcssEnclose = settingKey[Boolean]("Enclose in one big function. Default: false")
  val rtlcssIncludeSource = settingKey[Boolean]("Include the content of source files in the source map as the sourcesContent property. Default: false")
  val rtlcssMangle = settingKey[Boolean]("Enables name mangling. Default: true")
  val rtlcssMangleOptions = settingKey[Seq[String]]("Options for mangling such as sort, topLevel etc. Default: Nil")
  val rtlcssPreamble = settingKey[Option[String]]("Any preamble to include at the start of the output. Default: None")
  val rtlcssReserved = settingKey[Seq[String]]("Reserved names to exclude from mangling. Default: Nil")
  val rtlcssOps = settingKey[RtlcssOps.RtlcssOpsMethod]("A function defining how to combine input files into output files. Default: UglifyOps.singleFileWithSourceMapOut")

  object RtlcssOps {

    /** A list of input files mapping to a single output file. */
    case class RtlcssOpGrouping(inputFiles: Seq[PathMapping], outputFile: String, inputMapFile: Option[PathMapping], outputMapFile: Option[String])

    type RtlcssOpsMethod = (Seq[PathMapping]) => Seq[RtlcssOpGrouping]

    def dotRtl(file: String): String = {
      val exti = file.lastIndexOf('.')
      val (pfx, ext) = if (exti == -1) (file, "")
      else file.splitAt(exti)
      pfx + ".rtl" + ext
    }

    /** Use when rtlcssing single files */
    val singleFile: RtlcssOpsMethod = { mappings =>
      mappings.map(fp => RtlcssOpGrouping(Seq(fp), dotRtl(fp._2), None, None))
    }
    /** Use when uglifying single files and you want a source map out */
    val singleFileWithSourceMapOut: RtlcssOpsMethod = { mappings =>
      mappings.map(fp => RtlcssOpGrouping(Seq(fp), dotRtl(fp._2), None, Some(dotRtl(fp._2) + ".map")))
    }
  }

}

object SbtRtlcss extends AutoPlugin {

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtJsEngine.autoImport.JsEngineKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import SbtWeb.autoImport._
  import WebKeys._
  import autoImport._
  import RtlcssOps._

  implicit private class RichFile(val self: File) extends AnyVal {
    def startsWith(dir: File): Boolean = self.getPath.startsWith(dir.getPath)
  }

  override def projectSettings = Seq(
    rtlcssBuildDir := (resourceManaged in rtlcss).value / "build",
    rtlcssComments := None,
    rtlcssCompress := true,
    rtlcssCompressOptions := Nil,
    rtlcssDefine := None,
    rtlcssEnclose := false,
    excludeFilter in rtlcss :=
      HiddenFileFilter ||
        GlobFilter("*.rtl.css") ||
        new SimpleFileFilter({ file =>
          file.startsWith((WebKeys.webModuleDirectory in Assets).value)
        }),
    includeFilter in rtlcss := GlobFilter("*.css"),
    rtlcssIncludeSource := false,
    resourceManaged in rtlcss := webTarget.value / rtlcss.key.label,
    rtlcssMangle := true,
    rtlcssMangleOptions := Nil,
    rtlcssPreamble := None,
    rtlcssReserved := Nil,
    rtlcss := runOptimizer.dependsOn(webJarsNodeModules in Plugin).value,
    rtlcssOps := singleFileWithSourceMapOut
  )

  private def runOptimizer: Def.Initialize[Task[Pipeline.Stage]] = Def.task {
    val include = (includeFilter in rtlcss).value
    val exclude = (excludeFilter in rtlcss).value
    val buildDirValue = rtlcssBuildDir.value
    val rtlcssOpsValue = rtlcssOps.value
    val streamsValue = streams.value
    val nodeModuleDirectoriesInPluginValue = (nodeModuleDirectories in Plugin).value
    val webJarsNodeModulesDirectoryInPluginValue = (webJarsNodeModulesDirectory in Plugin).value
    val mangleValue = rtlcssMangle.value
    val mangleOptionsValue = rtlcssMangleOptions.value
    val reservedValue = rtlcssReserved.value
    val compressValue = rtlcssCompress.value
    val compressOptionsValue = rtlcssCompressOptions.value
    val encloseValue = rtlcssEnclose.value
    val includeSourceValue = rtlcssIncludeSource.value
    val timeout = (timeoutPerSource in rtlcss).value
    val stateValue = state.value
    val engineTypeInUglifyValue = (engineType in rtlcss).value
    val commandInUglifyValue = (command in rtlcss).value
    val options = Seq(
      rtlcssComments.value,
      compressValue,
      compressOptionsValue,
      rtlcssDefine.value,
      encloseValue,
      (excludeFilter in rtlcss).value,
      (includeFilter in rtlcss).value,
      (resourceManaged in rtlcss).value,
      mangleValue,
      mangleOptionsValue,
      rtlcssPreamble.value,
      reservedValue,
      includeSourceValue
    ).mkString("|")

    (mappings) => {
      val optimizerMappings = mappings.filter(f => !f._1.isDirectory && include.accept(f._1) && !exclude.accept(f._1))

      SbtWeb.syncMappings(
        Compat.cacheStore(streamsValue, "rtlcss-cache"),
        optimizerMappings,
        buildDirValue
      )
      val appInputMappings = optimizerMappings.map(p => rtlcssBuildDir.value / p._2 -> p._2)
      val groupings = rtlcssOpsValue(appInputMappings)

      implicit val opInputHasher: OpInputHasher[RtlcssOpGrouping] = OpInputHasher[RtlcssOpGrouping](io =>
        OpInputHash.hashString(
          (io.outputFile +: io.inputFiles.map(_._1.getAbsolutePath)).mkString("|") + "|" + options
        )
      )

      val (outputFiles, ()) = incremental.syncIncremental(streamsValue.cacheDirectory / "run", groupings) {
        modifiedGroupings: Seq[RtlcssOpGrouping] =>
          if (modifiedGroupings.nonEmpty) {

            streamsValue.log.info(s"Optimizing ${modifiedGroupings.size} Stylesheet(s) with rtlcss")

            val nodeModulePaths = nodeModuleDirectoriesInPluginValue.map(_.getPath)
            val rtlcssjsShell = webJarsNodeModulesDirectoryInPluginValue / "rtlcss" / "bin" / "rtlcss"

            def executeRtlcss(args: Seq[String]) = monix.eval.Task {
              SbtJsTask.executeJs(
                stateValue.copy(),
                engineTypeInUglifyValue,
                commandInUglifyValue,
                nodeModulePaths,
                rtlcssjsShell,
                args: Seq[String],
                timeout
              )
            }

            val resultObservable: Observable[(RtlcssOpGrouping, OpResult)] = Observable.fromIterable(
              modifiedGroupings
                .sortBy(_.inputFiles.map(_._1.length()).sum)
                .reverse
            ).map { grouping =>
              val inputFiles = grouping.inputFiles.map(_._1)
              val inputFileArgs = inputFiles.map(_.getPath)

              val outputFile = buildDirValue / grouping.outputFile
              IO.createDirectory(outputFile.getParentFile)
              val outputFileArgs = Seq(outputFile.getPath)

              val args =
              // Seq("--config", ".rtlcssrc") ++
                inputFileArgs ++ outputFileArgs

              executeRtlcss(args).map { result =>
                val success = result.headOption.fold(true)(_ => false)
                grouping -> (
                  if (success) {
                    OpSuccess(inputFiles.toSet, Set(outputFile))
                  }
                  else {
                    OpFailure
                  }
                  )
              }
            }.mergeMap(task => Observable.fromTask(task))

            val rtlcssPool = monix.execution.Scheduler.computation(
              parallelism = java.lang.Runtime.getRuntime.availableProcessors
            )
            val result = Await.result(
              resultObservable.toListL.runAsync(rtlcssPool),
              timeout * modifiedGroupings.size
            )

            (result.toMap, ())
          } else {
            (Map.empty, ())
          }
      }

      (mappings.toSet ++ outputFiles.pair(relativeTo(buildDirValue))).toSeq
    }
  }
}
