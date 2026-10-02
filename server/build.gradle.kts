import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
}

application {
    mainClass.set("com.guesswhosesong.server.ApplicationKt")
}

kotlin {
    jvmToolchain(17)
}

// Load .env if it exists for local development
val envFile = file(".env")
if (envFile.exists()) {
    val props = Properties()
    props.load(FileInputStream(envFile))
    tasks.withType<JavaExec> {
        props.forEach { (key, value) ->
            environment(key.toString(), value.toString())
        }
    }
}

dependencies {
    implementation(project(":shared"))

    // Ktor server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Ktor client (for Deezer + Spotify calls)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)

    // Redis
    implementation(libs.redis.jedis)
    implementation(libs.firebase.admin)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Logging
    implementation("ch.qos.logback:logback-classic:1.6.3")

    // Test
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("guess-whose-song-server")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
