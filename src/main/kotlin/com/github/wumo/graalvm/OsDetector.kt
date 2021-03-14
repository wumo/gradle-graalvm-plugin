package com.github.wumo.graalvm

import java.util.Locale

object OsDetector {
  private const val UNKNOWN = "unknown"

  val os: String
  val arch: String
  val classifier: String

  init {
    os = normalizeOs(System.getProperty("os.name"))
    arch = normalizeArch(System.getProperty("os.arch"))
    classifier="$os-$arch"
  }

  private fun normalize(value: String): String {
    return value.toLowerCase(Locale.US).replace("[^a-z0-9]+".toRegex(), "")
  }

  private fun normalizeOs(value: String): String {
    val v = normalize(value)
    return when {
      v.startsWith("aix") -> "aix"
      v.startsWith("hpux") -> "hpux"
      v.startsWith("os400") &&      // Avoid the names such as os4000
          (v.length <= 5 || !Character.isDigit(value[5])) -> "os400"
      v.startsWith("linux") -> "linux"
      v.startsWith("macosx") || v.startsWith("osx") -> "osx"
      v.startsWith("freebsd") -> "freebsd"
      v.startsWith("openbsd") -> "openbsd"
      v.startsWith("netbsd") -> "netbsd"
      v.startsWith("solaris") || v.startsWith("sunos") -> "sunos"
      v.startsWith("windows") -> "windows"
      v.startsWith("zos") -> "zos"
      else -> UNKNOWN
    }
  }

  private fun normalizeArch(value: String): String {
    val v = normalize(value)
    return when {
      v.matches(Regex("^(x8664|amd64|ia32e|em64t|x64)$")) -> "x86_64"
      v.matches(Regex("^(x8632|x86|i[3-6]86|ia32|x32)$")) -> "x86_32"
      v.matches(Regex("^(ia64w?|itanium64)$")) -> "itanium_64"
      "ia64n" == v -> "itanium_32"
      v.matches(Regex("^(sparc|sparc32)$")) -> "sparc_32"
      v.matches(Regex("^(sparcv9|sparc64)$")) -> "sparc_64"
      v.matches(Regex("^(arm|arm32)$")) -> "arm_32"
      "aarch64" == v -> "aarch_64"
      v.matches(Regex("^(mips|mips32)$")) -> "mips_32"
      v.matches(Regex("^(mipsel|mips32el)$")) -> "mipsel_32"
      "mips64" == v -> "mips_64"
      "mips64el" == v -> "mipsel_64"
      v.matches(Regex("^(ppc|ppc32)$")) -> "ppc_32"
      v.matches(Regex("^(ppcle|ppc32le)$")) -> "ppcle_32"
      "ppc64" == v -> "ppc_64"
      "ppc64le" == v -> "ppcle_64"
      "s390" == v -> "s390_32"
      "s390x" == v -> "s390_64"
      "riscv" == v -> "riscv"
      else -> UNKNOWN
    }
  }
}