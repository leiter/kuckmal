plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
                outputFileName = "mediathekview.js"
            }
            webpackTask {
                mainOutputFileName.set("mediathekview.js")
            }
            distribution {
                outputDirectory.set(File("${projectDir}/build/distributions"))
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.html.core)
                implementation(compose.runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.9.0")
            }
        }
    }
}

// Task to copy webOS files to distribution
tasks.register<Copy>("prepareWebOSPackage") {
    dependsOn("jsBrowserDistribution")

    from("src/jsMain/resources/webos") {
        include("appinfo.json")
        include("icon.png")
        include("largeIcon.png")
    }
    from("build/dist/js/productionExecutable") {
        include("**/*")
    }
    into("build/webos-package")
}

// Task to generate webOS IPK package (requires webOS CLI installed)
tasks.register<Exec>("packageWebOS") {
    dependsOn("prepareWebOSPackage")
    workingDir("build")
    commandLine("ares-package", "webos-package", "-o", "webos-output")

    doFirst {
        file("build/webos-output").mkdirs()
    }
}
