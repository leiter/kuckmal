plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("androidx.room") version "2.7.1"
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // Desktop JVM target
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    // JS target disabled for now
    // js(IR) {
    //     browser {
    //         commonWebpackConfig {
    //             cssSupport {
    //                 enabled.set(true)
    //             }
    //         }
    //     }
    //     binaries.executable()
    // }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

                // Compose Navigation KMP
                implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")

                // Lifecycle ViewModel KMP
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

                // Room KMP
                implementation("androidx.room:room-runtime:2.7.1")
                implementation("androidx.sqlite:sqlite-bundled:2.5.1")

                // Koin for KMP
                implementation("io.insert-koin:koin-core:4.0.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }

        // jsMain disabled for now
        // val jsMain by getting {
        //     dependencies {
        //         implementation(compose.html.core)
        //         implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.9.0")
        //     }
        // }
    }
}

android {
    namespace = "com.mediathekview.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkGeneratedSources = false
    }
}

// Room KMP configuration
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", "androidx.room:room-compiler:2.7.1")
    add("kspDesktop", "androidx.room:room-compiler:2.7.1")
    add("kspIosSimulatorArm64", "androidx.room:room-compiler:2.7.1")
    add("kspIosX64", "androidx.room:room-compiler:2.7.1")
    add("kspIosArm64", "androidx.room:room-compiler:2.7.1")
}
