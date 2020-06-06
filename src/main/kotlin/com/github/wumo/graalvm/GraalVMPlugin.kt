package com.github.wumo.graalvm

import com.google.gradle.osdetector.OsDetector
import net.lingala.zip4j.ZipFile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths

internal const val GRAALVM_NAME = "graalvm"

internal lateinit var config: GraalVMPluginExtension

fun Project.graalvm(block: GraalVMPluginExtension.()->Unit) {
  configure(block)
}

class GraalVMPlugin: Plugin<Project> {
  private val osDetector = OsDetector()
  
  private val isWindows = osDetector.os == "windows"
  private val arch = osDetector.arch
  private var vcvarsall = ""
  private var vcvarsall_arch = when(arch) {
    "x86"    -> "x86"
    "x86_64" -> "x64"
    else     -> ""
  }
  
  override fun apply(project: Project): Unit = project.run {
    config = extensions.create(GRAALVM_NAME)
    
    afterEvaluate {
      val jar = tasks.getByName<Jar>("jar")
      
      val nativeImage by tasks.registering {
        group = GRAALVM_NAME
        description = "Generate native image"
        dependsOn(jar)
        
        doLast {
          checkGraalVM()
          checkMSVC()
          
          val dstDir = project.buildDir.toPath().resolve(GRAALVM_NAME).toFile()
          dstDir.mkdirs()
          val graalvmBin = Paths.get(config.graalvmHome, "bin", "native-image").toAbsolutePath().toString()
          val cmd = buildString {
            if(isWindows)
              append("\"$vcvarsall\" $vcvarsall_arch && ")
            append("\"$graalvmBin\" ")
            append("-cp ")
            val paths = project.configurations.getByName("runtimeClasspath").files.toMutableList()
            paths.add(jar.outputs.files.singleFile)
            append("\"").append(paths.joinToString(File.pathSeparator) { it.absolutePath }).append("\" ")
            append("-H:Path=").append("\"${dstDir.absolutePath}\" ")
            val exeName =
              if(config.executableName.isNotBlank()) config.executableName.trim()
              else "${project.name}-${osDetector.classifier}"
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
    if(isWindows) {
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
    if(!exe.exists()) {
      val tmp = dstDir.resolve("$exePath.tmp")
      val conn = URL(url)
      conn.openStream().use { input->
        FileOutputStream(tmp).use { output->
          input.transferTo(output)
        }
      }
      if(unzip) {
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
    if(isWindows)
      exec(dir, "cmd", "/c", "call $cmd")
    else
      exec(dir, "/bin/bash", "-c", cmd)
  }
  
  fun eval(cmd: String, workDir: File? = null): String {
    val dir = workDir ?: File(".")
    return if(isWindows)
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
      return it.readText()
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
    check(process.exitValue() == 0) { "error exec $command" }
  }
}