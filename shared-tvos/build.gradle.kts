plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.1.0"
}

kotlin {
    // tvOS targets only
    listOf(
        tvosX64(),
        tvosArm64(),
        tvosSimulatorArm64()
    ).forEach { tvosTarget ->
        tvosTarget.binaries.framework {
            baseName = "SharedTvos"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

                // Koin for DI
                implementation("io.insert-koin:koin-core:4.0.1")

                // Ktor for HTTP client
                implementation("io.ktor:ktor-client-core:3.0.3")
                implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")

                // kotlinx.serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val tvosX64Main by getting
        val tvosArm64Main by getting
        val tvosSimulatorArm64Main by getting
        val tvosMain by creating {
            dependsOn(commonMain)
            tvosX64Main.dependsOn(this)
            tvosArm64Main.dependsOn(this)
            tvosSimulatorArm64Main.dependsOn(this)
            dependencies {
                // Ktor tvOS engine (Darwin-based)
                implementation("io.ktor:ktor-client-darwin:3.0.3")
            }
        }
    }
}
