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
        versionCode = 96
        versionName = "2.1.62"
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
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = "Cashiro Debug"
            if (slimDebug) {
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
