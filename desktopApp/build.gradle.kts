import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // Koin for DI
    implementation("io.insert-koin:koin-core:4.0.1")
}

compose.desktop {
    application {
        mainClass = "com.mediathekview.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)

            packageName = "MediathekView"
            packageVersion = "1.0.0"
            description = "MediathekView - Browse German public media libraries"
            vendor = "MediathekView"

            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
                debMaintainer = "mediathekview@example.com"
                menuGroup = "Multimedia"
                appCategory = "Video"
            }

            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                menuGroup = "MediathekView"
                upgradeUuid = "mediathekview-desktop-app"
            }

            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
                bundleID = "com.mediathekview.desktop"
            }
        }
    }
}
