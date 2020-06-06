![Release](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/github/wumo/gradle-graalvm-plugin/maven-metadata.xml.svg?label=com.github.wumo.graalvm)

This plugin eases the use of [graalvm](https://github.com/oracle/graal).

Inspired by [org.mikeneck.graalvm-native-image](https://github.com/mike-neck/graalvm-native-image-plugin)

## Usage
Enable plugin `com.github.wumo.graalvm` in your `build.gradle.kts`:
```kotlin
import org.bytedeco.javacpp.tools.Info

plugins {
  id("com.github.wumo.graalvm") version "0.0.1"
}

graalvm {
}
```

