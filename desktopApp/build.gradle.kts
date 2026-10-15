import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

kotlin {
    sourceSets {
        named("main") {
            kotlin.srcDir("src/jvmMain/kotlin")
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.mysterynumbers.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "神秘数字"
            packageVersion = "1.0.0"

            macOS {
                bundleID = "com.mysterynumbers"
                iconFile.set(project.file("src/main/resources/icons/mystery-numbers.icns"))
                infoPlist {
                    extraKeysRawXml = "<key>LSUIElement</key><true/>"
                }
            }

            windows {
                iconFile.set(project.file("src/main/resources/icons/mystery-numbers.ico"))
            }
        }
    }
}

tasks.register<Exec>("packageNativeMacApp") {
    dependsOn("createDistributable")
    onlyIf { System.getProperty("os.name").contains("Mac", ignoreCase = true) }
    commandLine("bash", rootProject.file("tools/package-macos-native.sh").absolutePath)
}

tasks.register<Exec>("packageNativeMacDmg") {
    dependsOn("packageNativeMacApp")
    onlyIf { System.getProperty("os.name").contains("Mac", ignoreCase = true) }
    commandLine("bash", rootProject.file("tools/package-macos-native-dmg.sh").absolutePath)
}

// Package the complete Windows app directory so users can run it without an installer.
tasks.register<Zip>("packagePortableZip") {
    dependsOn("createDistributable")
    onlyIf { System.getProperty("os.name").contains("Windows", ignoreCase = true) }

    val appDirectory = layout.buildDirectory.dir("compose/binaries/main/app")
    from(appDirectory) {
        include("神秘数字/**")
    }
    destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main/portable"))
    archiveBaseName.set("神秘数字")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("portable")
    includeEmptyDirs = true
}
