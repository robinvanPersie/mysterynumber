import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.Exec

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
