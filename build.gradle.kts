plugins {
  base
  `kotlin-dsl`
  `maven-publish`
  id("com.gradle.plugin-publish") version "0.13.0"
}

group = "com.github.wumo"
version = "0.0.8"

pluginBundle {
  website = "https://github.com/wumo/gradle-graalvm-plugin"
  vcsUrl = "https://github.com/wumo/gradle-graalvm-plugin.git"
  tags = listOf("graalvm", "native-image", "kotlin")
}

gradlePlugin {
  plugins {
    register("JavaCPPPlugin") {
      id = "com.github.wumo.graalvm"
      displayName = "graalvm"
      description = "graalvm plugin"
      implementationClass = "com.github.wumo.graalvm.GraalVMPlugin"
    }
  }
}

repositories {
  jcenter()
}

dependencies {
  implementation("net.lingala.zip4j:zip4j:2.7.0")
  implementation("com.github.jengelman.gradle.plugins:shadow:6.1.0")
  
  testImplementation("junit:junit:4.13")
  testImplementation(kotlin("test-junit"))
}

tasks {
  compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
  compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
}

val kotlinSourcesJar by tasks

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["kotlin"])
      artifact(kotlinSourcesJar)
    }
  }
}

