package com.github.wumo.graalvm

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertTrue

class GraalvmTest {
  
  @Test
  fun test() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply("com.github.wumo.graalvm")
  }
}