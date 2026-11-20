import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val appVersion = project.findProperty("app.versionName") as? String ?: "1.0.0"

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":composeApp"))
            implementation(compose.desktop.currentOs)
            implementation(compose.material3)
            implementation(compose.foundation)
            implementation("org.jetbrains.compose.desktop:desktop-jvm-linux-arm64:${libs.versions.composeMultiplatform.get()}")
            implementation("org.jetbrains.compose.desktop:desktop-jvm-linux-x64:${libs.versions.composeMultiplatform.get()}")
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation("org.slf4j:slf4j-nop:2.0.12")
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.solarad.app.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "AdDash"
            packageVersion = appVersion
            description = "AdDash - AdMob Analytics Dashboard"
            copyright = "© 2026 AdDash"
            vendor = "AdDash"

            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icons/icon.icns"))
                bundleID = "com.lavish.addash"
            }
        }
    }
}

// Tasks for Raspberry Pi (Linux ARM64) Distribution & Packaging
val prepareRaspberryPiLibs by tasks.registering(Sync::class) {
    dependsOn("jvmJar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val jvmRuntimeClasspath = kotlin.jvm().compilations.getByName("main").runtimeDependencyFiles
    val jvmJar = tasks.named("jvmJar", org.gradle.jvm.tasks.Jar::class)

    from(jvmRuntimeClasspath) {
        // Exclude non-ARM64 native libraries from Raspberry Pi package
        exclude("**/skiko-awt-runtime-macos-*")
        exclude("**/skiko-awt-runtime-linux-x64-*")
        exclude("**/skiko-awt-runtime-windows-*")
    }
    from(jvmJar.map { it.archiveFile })
    into(layout.buildDirectory.dir("pi-libs"))
}

tasks.register<Tar>("packageRaspberryPiTar") {
    dependsOn(prepareRaspberryPiLibs)
    group = "distribution"
    description = "Packages AdDash into a standalone tar.gz bundle for Raspberry Pi / Linux ARM64"

    archiveBaseName.set("addash")
    archiveVersion.set(appVersion)
    archiveClassifier.set("raspberry-pi-arm64")
    archiveExtension.set("tar.gz")
    compression = Compression.GZIP
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    into("addash") {
        into("bin") {
            from(file("src/jvmMain/resources/linux/addash.sh")) {
                rename { "addash" }
                filePermissions {
                    unix("rwxr-xr-x")
                }
            }
        }
        into("lib") {
            from(layout.buildDirectory.dir("pi-libs"))
        }
        into("share/applications") {
            from(file("src/jvmMain/resources/linux/addash.desktop"))
        }
        into("share/icons") {
            from(file("src/jvmMain/resources/icons/icon.png")) {
                rename { "addash.png" }
            }
        }
    }
}

tasks.register<Exec>("packageRaspberryPiDeb") {
    dependsOn(prepareRaspberryPiLibs)
    group = "distribution"
    description = "Packages AdDash into a Debian .deb package installable on Raspberry Pi (ARM64)"

    val outputDeb = layout.buildDirectory.file("distributions/addash_${appVersion}_arm64.deb")

    commandLine(
        "python3",
        file("package_deb.py").absolutePath,
        "--lib-dir", layout.buildDirectory.dir("pi-libs").get().asFile.absolutePath,
        "--launcher", file("src/jvmMain/resources/linux/addash.sh").absolutePath,
        "--desktop", file("src/jvmMain/resources/linux/addash.desktop").absolutePath,
        "--control", file("src/jvmMain/resources/linux/control").absolutePath,
        "--icon", file("src/jvmMain/resources/icons/icon.png").absolutePath,
        "--output", outputDeb.get().asFile.absolutePath
    )
}
