import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

// Automatically restore the original debug.keystore from base64 if it is missing
val b64File = if (file("/debug.keystore.base64").exists()) file("/debug.keystore.base64") else file("${rootDir}/debug.keystore.base64")
if (b64File.exists()) {
  val decodedBytes: ByteArray? = try {
    val base64Content = b64File.readText().replace("\\s".toRegex(), "").trim()
    Base64.getDecoder().decode(base64Content)
  } catch (e: Exception) {
    try {
      Base64.getMimeDecoder().decode(b64File.readText().trim())
    } catch (e2: Exception) {
      null
    }
  }
  if (decodedBytes != null) {
    listOf(file("${rootDir}/debug.keystore"), file("/debug.keystore")).forEach { f ->
      try {
        if (!f.exists() || f.length() == 0L) {
          f.parentFile?.mkdirs()
          f.writeBytes(decodedBytes)
          println("Successfully restored keystore to: ${f.absolutePath}")
        }
      } catch (e: Exception) {
        println("Could not write keystore to ${f.absolutePath}: ${e.message}")
      }
    }
  }
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.vibplay.axelixx"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation("androidx.media:media:1.7.0")
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("copyApkToRoot") {
  doLast {
    val src = file("${layout.buildDirectory.get()}/outputs/apk/debug/app-debug.apk")
    val dest = file("${rootDir}/app-debug.apk")
    val altDest = file("/app-debug.apk")
    println("--- COPY APK DIAGNOSTICS ---")
    println("Source path: ${src.absolutePath}")
    println("Source exists: ${src.exists()}")
    if (src.exists()) {
      println("Source size: ${src.length()} bytes")
      src.copyTo(dest, overwrite = true)
      println("Copy to ${dest.absolutePath} completed successfully! Size: ${dest.length()} bytes")
      try {
        if (altDest.absolutePath != dest.absolutePath) {
          src.copyTo(altDest, overwrite = true)
          println("Copy to ${altDest.absolutePath} completed successfully! Size: ${altDest.length()} bytes")
        }
      } catch (e: Exception) {
        println("Could not copy to alt path ${altDest.absolutePath}: ${e.message}")
      }
    } else {
      println("WARNING: SOURCE APK DOES NOT EXIST, COPY SKIPPED!")
    }
    println("----------------------------")
  }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
  finalizedBy("copyApkToRoot")
}

