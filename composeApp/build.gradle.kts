import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { (this as Properties).load(it) }
    }
}
val mixpanelToken: String = (project.findProperty("mixpanel.token") as? String)
    ?: localProperties.getProperty("mixpanel.token")
    ?: System.getenv("MIXPANEL_TOKEN")
    ?: ""

val generatedOutputDir = layout.buildDirectory.dir("generated/source/mixpanel/kotlin")

val generateMixpanelConfig by tasks.registering {
    inputs.property("mixpanelToken", mixpanelToken)
    outputs.dir(generatedOutputDir)
    doLast {
        val targetFile = generatedOutputDir.get().file("com/solarad/app/analytics/MixpanelConfig.kt").asFile
        targetFile.parentFile.mkdirs()
        targetFile.writeText(
            """
            package com.solarad.app.analytics

            /**
             * Auto-generated Mixpanel configuration.
             * DO NOT commit sensitive values to version control.
             * This file is generated dynamically from local.properties or MIXPANEL_TOKEN env var.
             */
            object MixpanelConfig {
                const val TOKEN: String = "$mixpanelToken"
                val isConfigured: Boolean get() = TOKEN.isNotBlank()
            }
            """.trimIndent()
        )
    }
}

kotlin {
    androidLibrary {
        namespace = "com.lavish.admob"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        withHostTest {}

        androidResources {
            enable = true
        }

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "com.lavish.admob.ComposeApp")
        }
    }

    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(compose.desktop.currentOs)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.multiplatform.settings.test)
        }
        commonMain {
            kotlin.srcDir(generateMixpanelConfig)
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.compose.material.icons.core)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                api(libs.multiplatform.settings)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.solarad.app.resources"
    generateResClass = always
}
