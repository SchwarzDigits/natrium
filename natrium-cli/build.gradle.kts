/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

val backendProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun backendProp(key: String): String =
    backendProps.getProperty(key) ?: error("Missing '$key' in local.properties")

val generateBackendConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/backendConfig")
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("schwarz/digits/natrium/cli")
        dir.mkdirs()
        dir.resolve("BackendProperties.kt").writeText(
            """
            |package schwarz.digits.natrium.cli
            |
            |internal object BackendProperties {
            |    const val NAME = "${backendProp("backend.name")}"
            |    const val API = "${backendProp("backend.api")}"
            |    const val ACCOUNTS = "${backendProp("backend.accounts")}"
            |    const val WEB_SOCKET = "${backendProp("backend.webSocket")}"
            |    const val TEAMS = "${backendProp("backend.teams")}"
            |    const val BLACK_LIST = "${backendProp("backend.blackList")}"
            |    const val WEBSITE = "${backendProp("backend.website")}"
            |}
            """.trimMargin()
        )
    }
}

kotlin {
    jvmToolchain(17)
    sourceSets["main"].kotlin.srcDir(generateBackendConfig.map { it.outputs.files.singleFile })
}

group = "schwarz.digits"
version = "0.1.0-SNAPSHOT"

application {
    mainClass.set("schwarz.digits.natrium.cli.MainKt")
}

dependencies {
    // Natrium Core (unser SDK)
    implementation(project(":natrium-core"))

    // CLI Framework
    implementation(libs.cliKt)

    // Coroutines (for runBlocking)
    implementation(libs.coroutines.core)

    // DateTime (for ChatMessage.timestamp)
    implementation(libs.kotlinx.datetime)

    // Test
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "schwarz.digits.natrium.cli.MainKt"
    }
    // Fat JAR for easy execution
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({ configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } })
}
