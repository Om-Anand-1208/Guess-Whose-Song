import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

compose.resources {
    generateResClass = always
    publicResClass = true
    packageOfResClass = "com.guesswhosesong.web.generated.resources"
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                devServer = KotlinWebpackConfig.DevServer(open = false, port = 5173).apply {
                    client = KotlinWebpackConfig.DevServer.Client(
                        overlay = KotlinWebpackConfig.DevServer.Client.Overlay(errors = true, warnings = false)
                    )
                    static("kotlin", watch = true)
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.uiUtil)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.coil3.compose)
                implementation(libs.coil3.network.ktor3)
                implementation(libs.ktor.client.js)
            }
        }
        val wasmJsTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

tasks.named<KotlinWebpack>("wasmJsBrowserDevelopmentWebpack") {
    devtool = "source-map"
}

tasks.named<KotlinWebpack>("wasmJsBrowserDevelopmentRun") {
    devtool = "source-map"
}
