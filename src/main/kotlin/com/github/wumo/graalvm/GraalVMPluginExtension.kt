package com.github.wumo.graalvm

open class GraalVMPluginExtension {
  var graalvmHome: String = System.getProperty("java.home")
  var mainClassName: String = ""
  var executableName: String = ""
  var arguments: List<String> = mutableListOf()
}