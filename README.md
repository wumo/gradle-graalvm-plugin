![Release](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/github/wumo/gradle-graalvm-plugin/maven-metadata.xml.svg?label=com.github.wumo.graalvm)

This plugin eases the use of [graalvm](https://github.com/oracle/graal).

Inspired by [org.mikeneck.graalvm-native-image](https://github.com/mike-neck/graalvm-native-image-plugin)

## Usage

Make sure you have installed native compilation toolchain such as gcc or Visual Studio 2019 (or above)

Enable plugin `com.github.wumo.graalvm` in your `build.gradle.kts`:
```kotlin
import org.bytedeco.javacpp.tools.Info

plugins {
  id("com.github.wumo.graalvm") version "0.0.1"
}

graalvm {
  graalvmHome = System.getenv("GRAALVM_HOME")
    mainClassName = "com.github.wumo.MainKt"
    arguments = listOf(
      "--no-fallback",
      "--enable-all-security-services",
      "--report-unsupported-elements-at-runtime",
      "--allow-incomplete-classpath"
    )
}
```

