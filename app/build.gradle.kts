import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
}

fun gitCommitCount(): Int {
    project.findProperty("gitCommitCount")?.toString()?.toIntOrNull()?.let { return it }
    return runCatching {
        ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .directory(rootProject.projectDir)
            .redirectErrorStream(true)
            .start()
            .inputStream.bufferedReader().use { it.readText().trim().toInt() }
    }.getOrDefault(0)
}

fun gitSha(): String {
    project.findProperty("gitSha")?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
    return runCatching {
        ProcessBuilder("git", "rev-parse", "--short=7", "HEAD")
            .directory(rootProject.projectDir)
            .redirectErrorStream(true)
            .start()
            .inputStream.bufferedReader().use { it.readText().trim() }
    }.getOrDefault("unknown")
}

val slimDebug = project.hasProperty("slimDebug")

android {
    namespace = "com.ritesh.cashiro"
    compileSdk = 37
    buildFeatures {
        buildConfig = true
        compose = true
    }
    defaultConfig {
        applicationId = "com.ritesh.cashiro"
        manifestPlaceholders["appLabel"] = "@string/app_name"
        minSdk = 26
        targetSdk = 36
        versionCode = 97
        versionName = "2.1.63"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val commitCount = gitCommitCount()
        val sha = gitSha()
        buildConfigField("int", "GIT_COMMIT_COUNT", commitCount.toString())
        buildConfigField("String", "GIT_SHA", "\"$sha\"")
        if (slimDebug) {
            ndk { abiFilters += "arm64-v8a" }
        }
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            val localProperties = Properties()
            localProperties.load(localPropertiesFile.inputStream())
            val rsaPublicKey = localProperties.getProperty("RSA_PUBLIC_KEY", "")
            buildConfigField("String", "RSA_PUBLIC_KEY", "\"$rsaPublicKey\"")
        } else {
            buildConfigField("String", "RSA_PUBLIC_KEY", "\"\"")
        }
    }
    signingConfigs {
        getByName("debug") {
            // CI must supply the persistent, debug-only identity. Local IDE builds
            // may still use their own default debug key unless explicitly required.
            val names = listOf("DEBUG_STORE_FILE", "DEBUG_STORE_PASSWORD", "DEBUG_KEY_ALIAS", "DEBUG_KEY_PASSWORD")
            val values = names.associateWith { providers.environmentVariable(it).orNull }
            val configured = values.values.any { !it.isNullOrBlank() }
            if (configured || project.hasProperty("requireDebugSigning")) {
                val missing = names.filter { values[it].isNullOrBlank() }
                check(missing.isEmpty()) { "Missing persistent debug signing settings: ${missing.joinToString()}" }
                val keyFile = rootProject.file(values.getValue("DEBUG_STORE_FILE")!!)
                check(keyFile.isFile) { "Persistent debug keystore does not exist" }
                storeFile = keyFile
                storePassword = values.getValue("DEBUG_STORE_PASSWORD")
                keyAlias = values.getValue("DEBUG_KEY_ALIAS")
                keyPassword = values.getValue("DEBUG_KEY_PASSWORD")
            }
        }
        create("release") {
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                val localProperties = Properties()
                localProperties.load(localPropertiesFile.inputStream())
                val keystorePath = localProperties.getProperty("RELEASE_STORE_FILE", "")
                if (keystorePath.isNotEmpty()) {
                    storeFile = file(keystorePath)
                    storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD", "")
                    keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS", "")
                    keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD", "")
                }
            }
        }
    }
    flavorDimensions += "version"
    productFlavors {
        create("fdroid") {
            dimension = "version"
            ndk { abiFilters += setOf("arm64-v8a", "armeabi-v7a") }
        }
        create("standard") {
            dimension = "version"
            isDefault = true
        }
    }
    splits {
        abi {
            val runTasks = gradle.startParameter.taskNames.map { it.lowercase() }
            val isBundleBuild = runTasks.any { it.contains("bundle") }
            val isFdroidBuild = runTasks.any { it.contains("fdroid") }
            isEnable = !slimDebug && !isBundleBuild && !isFdroidBuild
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = !slimDebug
        }
    }
    buildTypes {
        debug {
            buildConfigField("String", "UPDATE_CHANNEL", "\"debug\"")
            applicationIdSuffix = ".debug"
            // "-debug+<short sha>" (see androidComponents below)
            versionNameSuffix = gitSha().let { if (it == "unknown") "-debug" else "-debug+$it" }
            manifestPlaceholders["appLabel"] = "Cashiro Debug"
            if (slimDebug) {
                // The CI build people install day to day: not debuggable, so ART compiles it
                // ahead of time with the Baseline Profile like a release (a debuggable app
                // only ever runs interpreted/JIT and gets no profile). Local debug builds
                // without -PslimDebug stay debuggable.
                isDebuggable = false
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }
        release {
            buildConfigField("String", "UPDATE_CHANNEL", "\"release\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            ndk { debugSymbolLevel = "SYMBOL_TABLE" }
        }
        // AGP forbids build type names that start with "test".
        // The GitHub / in-app channel is still "testing".
        create("preview") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            versionNameSuffix = "-testing"
            manifestPlaceholders["appLabel"] = "Cashiro Testing"
            buildConfigField("String", "UPDATE_CHANNEL", "\"testing\"")
            signingConfig = signingConfigs.getByName("release")
        }
        // Release code (R8, no debuggable overhead) that :benchmark can drive.
        // src/benchmark adds <profileable> and a data-seeding activity.
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            // -PbenchmarkIdSuffix=.benchmark.base builds a baseline that installs next to
            // the candidate, so a device farm can measure both in one run.
            applicationIdSuffix = providers.gradleProperty("benchmarkIdSuffix").getOrElse(".benchmark")
            versionNameSuffix = "-benchmark"
            manifestPlaceholders["appLabel"] = "Cashiro Benchmark"
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles("benchmark-rules.pro")
        }
        // The benchmark build without R8, for BaselineProfileRule: the profile then names the
        // app's own classes and methods, and R8 maps them when building release.
        create("profiling") {
            initWith(getByName("benchmark"))
            matchingFallbacks += listOf("benchmark", "release")
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    // Same seeding activity and <profileable> as the benchmark build.
    sourceSets.getByName("profiling") {
        java.srcDir("src/benchmark/java")
        manifest.srcFile("src/benchmark/AndroidManifest.xml")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }
    packaging {
        jniLibs { useLegacyPackaging = true }
        resources {
            excludes += setOf("META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/*.kotlin_module")
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    testOptions { unitTests.isReturnDefaultValues = true }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        lintConfig = file("lint.xml")
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }
}

androidComponents {
    // Every debug build carries its commit: the commit count as versionCode, so the installer
    // and Settings show which build is installed, and "+<short sha>" after "-debug" in the
    // versionName. The debug app has its own applicationId, so this never meets release codes.
    onVariants(selector().withBuildType("debug")) { variant ->
        val count = gitCommitCount()
        if (count > 0) variant.outputs.forEach { it.versionCode.set(count) }
    }
    onVariants(selector().withBuildType("preview")) { variant ->
        val count = gitCommitCount().coerceAtLeast(1)
        variant.outputs.forEach { output ->
            output.versionCode.set(count)
        }
    }
}

// ./gradlew :app:compileStandardReleaseKotlin -PcomposeReports
// writes stability reports and metrics to app/build/compose_compiler.
if (project.hasProperty("composeReports")) {
    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(project(":parser-core"))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.colorpicker.compose)
    implementation(libs.haze)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.profileinstaller)
    // Composable names in Perfetto traces when Macrobenchmark runs with fullTracing.enable.
    "benchmarkImplementation"(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.gson)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
    implementation(libs.play.services.auth)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    "standardImplementation"(libs.app.update)
    "standardImplementation"(libs.app.update.ktx)
    "standardImplementation"(libs.review)
    "standardImplementation"(libs.review.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.opencsv)
    testImplementation(kotlin("test"))
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.gif)
    implementation(libs.compose.charts)
    implementation(libs.reorderable)
    implementation(libs.pdfbox.android)
}
