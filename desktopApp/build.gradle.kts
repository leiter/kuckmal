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

    // Lifecycle ViewModel KMP
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // Room KMP for desktop
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.sqlite:sqlite-bundled:2.5.1")

    // Koin for DI
    implementation("io.insert-koin:koin-core:4.0.1")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // XZ compression for film list
    implementation("org.tukaani:xz:1.9")
}

compose.desktop {
    application {
        mainClass = "cut.the.crap.desktop.MainKt"

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
                bundleID = "cut.the.crap.desktop"
            }
        }
    }
}
