import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

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
            implementation("org.jetbrains.compose.desktop:desktop-jvm-linux-arm64:${libs.versions.composeMultiplatform.get()}")
            implementation("org.jetbrains.compose.desktop:desktop-jvm-linux-x64:${libs.versions.composeMultiplatform.get()}")
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.solarad.app.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "SolarAd"
            packageVersion = "1.0.0"
            description = "SolarAd - AdMob Analytics Dashboard"
            copyright = "© 2026 SolarAd"
            vendor = "SolarAd"

            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icons/icon.icns"))
                bundleID = "com.solarad.app"
            }
        }
    }
}

// Tasks for Raspberry Pi (Linux ARM64) Distribution & Packaging
val prepareRaspberryPiLibs by tasks.registering(Sync::class) {
    dependsOn("jvmJar")
    val jvmRuntimeClasspath = kotlin.jvm().compilations.getByName("main").runtimeDependencyFiles
    val jvmJar = tasks.named("jvmJar", org.gradle.jvm.tasks.Jar::class)

    from(jvmRuntimeClasspath)
    from(jvmJar.map { it.archiveFile })
    into(layout.buildDirectory.dir("pi-libs"))
}

tasks.register<Tar>("packageRaspberryPiTar") {
    dependsOn(prepareRaspberryPiLibs)
    group = "distribution"
    description = "Packages SolarAd into a standalone tar.gz bundle for Raspberry Pi / Linux ARM64"

    archiveBaseName.set("solarad")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("raspberry-pi-arm64")
    archiveExtension.set("tar.gz")
    compression = Compression.GZIP
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    into("solarad") {
        into("bin") {
            from(file("src/jvmMain/resources/linux/solarad.sh")) {
                rename { "solarad" }
                filePermissions {
                    unix("rwxr-xr-x")
                }
            }
        }
        into("lib") {
            from(layout.buildDirectory.dir("pi-libs"))
        }
        into("share/applications") {
            from(file("src/jvmMain/resources/linux/solarad.desktop"))
        }
        into("share/icons") {
            from(file("src/jvmMain/resources/icons/icon.png")) {
                rename { "solarad.png" }
            }
        }
    }
}

tasks.register<Exec>("packageRaspberryPiDeb") {
    dependsOn(prepareRaspberryPiLibs)
    group = "distribution"
    description = "Packages SolarAd into a Debian .deb package installable on Raspberry Pi (ARM64)"

    val outputDeb = layout.buildDirectory.file("distributions/solarad_1.0.0_arm64.deb")

    commandLine(
        "python3",
        file("package_deb.py").absolutePath,
        "--lib-dir", layout.buildDirectory.dir("pi-libs").get().asFile.absolutePath,
        "--launcher", file("src/jvmMain/resources/linux/solarad.sh").absolutePath,
        "--desktop", file("src/jvmMain/resources/linux/solarad.desktop").absolutePath,
        "--control", file("src/jvmMain/resources/linux/control").absolutePath,
        "--icon", file("src/jvmMain/resources/icons/icon.png").absolutePath,
        "--output", outputDeb.get().asFile.absolutePath
    )
}
