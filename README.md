sbt-rtlcss
==========

An sbt-web plugin to perform [RTLCSS conversion](https://github.com/MohammadYounes/rtlcss/) on css files.

Usage
-----
To use this plugin, use the addSbtPlugin command within your project's `plugins.sbt` file:

```scala
addSbtPlugin("com.github.enalmada" % "sbt-rtlcss" % "0.1.0")
```

Your project's build file also needs to enable sbt-web plugins. For example, with build.sbt:

```scala
lazy val root = (project in file(".")).enablePlugins(SbtWeb)
```

As with all sbt-web asset pipeline plugins you must declare their order of execution:

```scala
pipelineStages in Assets := Seq(rtlcss)
```

A standard build profile for the Uglify optimizer is provided which will mangle variables for obfuscation and
compression. Each input file found in your assets folders matching the includeFilter will have a corresponding `.rtl.css` file.

## includeFilter

If you wish to limit or extend what is rtl then you can use filters:
```scala
includeFilter in rtlcss := GlobFilter("mycss/*.css"),
```
...where the above will include only those files under the `mycss` folder.

The sbt `excludeFilter` is also available to the `rtlcss` scope and defaults to excluding the public folder and extracted Webjars.
I am not sure it is working quite right if you override this though.  

##TODO (help wanted)
- This is the first release and quick and dirty copy of sbt-uglify to give me some rtl css files. Some polish would be nice.
- Add build.sbt options reflecting all the native rtlcss options
- Figure out (and document for others) how to run the tests.  I went into sbt-test/sbt-rtlcss/rtlcss and did "sbt clean webStage" manually for now
- Figure out how to remove the "ERROR default configuration file used" message
- Figure out wy using exclude filter seems to enable much more rtl css.

The plugin is built on top of [JavaScript Engine](https://github.com/typesafehub/js-engine) which supports different JavaScript runtimes.


