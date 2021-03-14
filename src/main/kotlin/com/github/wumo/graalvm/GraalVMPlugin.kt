package com.github.wumo.graalvm

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.lingala.zip4j.ZipFile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.jvm.Jvm
import org.gradle.kotlin.dsl.*
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths

internal const val GRAALVM_NAME = "graalvm"

open class GraalVMPluginExtension {
  var graalvmHome: String? = null
  var mainClassName: String = ""
  var executableName: String = ""
  var arguments: List<String> = mutableListOf()
}

fun Project.graalvm(block: GraalVMPluginExtension.() -> Unit) {
  configure(block)
}

class GraalVMPlugin : Plugin<Project> {
  private val isWindows = OsDetector.os == "windows"
  private val arch = OsDetector.arch
  private var vcvarsall = ""
  private var vcvarsall_arch = when (arch) {
    "x86" -> "x86"
    "x86_64" -> "x64"
    else -> ""
  }

  private lateinit var config: GraalVMPluginExtension

  override fun apply(project: Project): Unit = project.run {
    apply(plugin = "com.github.johnrengelman.shadow")
    config = extensions.create(GRAALVM_NAME)

    afterEvaluate {
      val jar = tasks.getByName<ShadowJar>("shadowJar")
      val dstDir = project.buildDir.toPath().resolve(GRAALVM_NAME).toFile()
      val exeName = if (config.executableName.isNotBlank()) config.executableName.trim()
      else "${project.name}-${project.version}-${OsDetector.classifier}"
      config.graalvmHome = config.graalvmHome ?: Jvm.current().javaHome.absolutePath

      val nativeImage by tasks.registering {
        group = GRAALVM_NAME
        description = "Generate native image"
        dependsOn(jar)

        doLast {
          checkGraalVM()
          checkMSVC()

          dstDir.mkdirs()

          val graalvmBin = Paths.get(config.graalvmHome, "bin", "native-image").toAbsolutePath().toString()
          val cmd = buildString {
            if (isWindows)
              append("\"$vcvarsall\" $vcvarsall_arch && ")
            append("\"$graalvmBin\" ")
            append("-cp ")
//            val paths = project.configurations.getByName("runtimeClasspath").files.toMutableList()
            val paths = mutableListOf<File>()
            paths.add(jar.outputs.files.singleFile)
            append("\"").append(paths.joinToString(File.pathSeparator) { it.absolutePath }).append("\" ")
            append("-H:Path=").append("\"${dstDir.absolutePath}\" ")
            append("-H:Name=$exeName ")
            config.arguments.forEach {
              append(it.trim()).append(" ")
            }
            append(config.mainClassName.trim())
          }
          println(cmd)
          exec(cmd)
        }
      }

      tasks.register<Zip>("nativeImageZip") {
        group = GRAALVM_NAME
        description = "zip compress native image"
        dependsOn(nativeImage)
        val fileExt = if (isWindows) ".exe" else ""
        val fileName = "$exeName$fileExt"
        archiveFileName.set("$exeName.zip")
        destinationDirectory.set(file("$buildDir/dist"))

        from(dstDir.resolve(fileName))
      }
    }
  }

  private fun checkGraalVM() {
    val graalvmHome = File(config.graalvmHome)
    check(graalvmHome.exists()) { "graalvm is missing" }
    val guPath = Paths.get(config.graalvmHome, "bin", "gu").toString()
    exec("\"$guPath\" install native-image")
    check(config.mainClassName.isNotBlank()) { "mainClassName is blank" }
  }

  private fun checkMSVC() {
    if (isWindows) {
      val cacheDir = Paths.get(System.getProperty("user.home"), ".graalvm", "cache")
        .toAbsolutePath().toFile()
      cacheDir.mkdirs()

      val vswhere = downloadIfNotExists(
        cacheDir,
        "vswhere.exe",
        "https://github.com/microsoft/vswhere/releases/download/2.8.4/vswhere.exe"
      ).toString()
      val version = eval("\"$vswhere\" -latest -property installationVersion").trim()
      check(version.isNotBlank()) { "msvc is missing" }
      val msvcRoot = eval("\"$vswhere\" -latest -property installationPath").trim()
      vcvarsall = Paths.get(msvcRoot, "VC/Auxiliary/Build/vcvarsall.bat").toString()
      check(File(vcvarsall).exists()) { "vcvarsall.bat is missing!" }
    }
  }

  private fun downloadIfNotExists(
    dstDir: File, exePath: String, url: String,
    unzip: Boolean = false
  ): File {
    val exe = dstDir.resolve(exePath)
    if (!exe.exists()) {
      val tmp = dstDir.resolve("$exePath.tmp")
      val conn = URL(url)
      conn.openStream().use { input ->
        FileOutputStream(tmp).use { output ->
          input.transferTo(output)
        }
      }
      if (unzip) {
        ZipFile(tmp).extractFile(exePath, dstDir.toString())
        tmp.delete()
      } else
        Files.move(tmp.toPath(), exe.toPath())
      println("downloaded $exe from $url")
    }
    return exe
  }

  fun exec(cmd: String, workDir: File? = null) {
    val dir = workDir ?: File(".")
    if (isWindows)
      exec(dir, "cmd", "/c", "call $cmd")
    else
      exec(dir, "/bin/bash", "-c", cmd)
  }

  fun eval(cmd: String, workDir: File? = null): String {
    val dir = workDir ?: File(".")
    return if (isWindows)
      call(dir, "cmd", "/c", "call $cmd")
    else
      call(dir, "/bin/bash", "-c", cmd)
  }

  fun call(workDir: File, vararg command: String): String {
    val builder = ProcessBuilder(*command)
    builder.directory(workDir)
    builder.redirectErrorStream(true)
    val process: Process = builder.start()
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    reader.use {
      return it.readText().apply {
        process.waitFor()
        check(process.exitValue() == 0) { "error exec $command" }
      }
    }
  }

  fun exec(workDir: File, vararg command: String) {
    val builder = ProcessBuilder(*command)
    builder.directory(workDir)
    builder.redirectErrorStream(true)
    val process = builder.start()
    val input = process.inputStream
    input.use {
      it.transferTo(System.out)
    }
    process.waitFor()
    check(process.exitValue() == 0) { "error exec $command" }
  }
}