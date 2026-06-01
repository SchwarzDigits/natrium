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

import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.mavenPublish)
}

group = "schwarz.opensource.natrium"
version = providers.gradleProperty("version").getOrElse("0.0.0-SNAPSHOT")

kotlin {
    jvmToolchain(17)

    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation("schwarz.opensource.natrium:logic:0.0.2-fork.1")
            api(libs.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.okio.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.test)
        }
        androidMain.dependencies {
            implementation("androidx.lifecycle:lifecycle-process:2.8.7")
        }

        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP's default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }
    }
}

android {
    namespace = "schwarz.digits.natrium"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

mavenPublishing {
    coordinates(group.toString(), "natrium-core", version.toString())
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    // NB: signAllPublications() bewusst weggelassen — triggert in vanniktech 0.30
    // unter Gradle 9 einen "value is final"-Fehler. Signing wird unten manuell verdrahtet.
    pom {
        name.set("natrium-core")
        description.set("Natrium: Kotlin Multiplatform wrapper around the Kalium messaging SDK")
        url.set("https://github.com/SchwarzDigits/natrium")
        licenses {
            license {
                name.set("GNU General Public License v3.0")
                url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("schwarzdigits")
                name.set("Schwarz Digits")
                organization.set("Schwarz Digits KG")
                organizationUrl.set("https://schwarz-it.com")
            }
        }
        scm {
            url.set("https://github.com/SchwarzDigits/natrium")
            connection.set("scm:git:git://github.com/SchwarzDigits/natrium.git")
            developerConnection.set("scm:git:ssh://git@github.com/SchwarzDigits/natrium.git")
        }
    }
}

// Manuelle Signing-Konfiguration (umgeht vanniktech 0.30 + Gradle 9 "property is final"-Bug).
// Werte kommen via ORG_GRADLE_PROJECT_* aus dem Workflow oder ~/.gradle/gradle.properties.
plugins.apply("signing")
val signingKey = providers.gradleProperty("signingInMemoryKey").orNull
val signingKeyPassword = providers.gradleProperty("signingInMemoryKeyPassword").orNull.orEmpty()
val signingKeyId = providers.gradleProperty("signingInMemoryKeyId").orNull
if (signingKey != null) {
    extensions.configure<org.gradle.plugins.signing.SigningExtension>("signing") {
        if (signingKeyId != null) {
            useInMemoryPgpKeys(signingKeyId, signingKey, signingKeyPassword)
        } else {
            useInMemoryPgpKeys(signingKey, signingKeyPassword)
        }
    }
    afterEvaluate {
        extensions.configure<org.gradle.plugins.signing.SigningExtension>("signing") {
            sign(extensions.getByType<org.gradle.api.publish.PublishingExtension>().publications)
        }
    }
}
