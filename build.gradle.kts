import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)

    application
}

tasks.jar { enabled = false }

group = "ru.sliva"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://mvn.mchv.eu/repository/mchv/")
}

dependencies {
    implementation(platform(libs.tdlight.bom))

    implementation(
        group = "it.tdlight",
        name = "tdlight-java"
    )

    implementation(
        group = "it.tdlight",
        name = "tdlight-natives",
        classifier = "macos_amd64"
    )

    implementation(libs.kotlinx.coroutines)
    implementation(libs.logback)
}

sourceSets.main {
    kotlin.srcDir("src")
    resources.srcDir("resources")
}

kotlin.compilerOptions {
    jvmTarget.set(JvmTarget.JVM_21)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 21
}

tasks.withType<Jar> {
    destinationDirectory = file("$rootDir/build")
}

application {
    mainClass = "ru.sliva.userbot.Bot"
}
