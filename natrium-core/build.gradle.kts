/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the European Union Public Licence (EUPL) v1.2.
 * See the LICENSE file in the project root for the full licence text.
 *
 * SPDX-License-Identifier: EUPL-1.2
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
                name.set("European Union Public Licence v1.2")
                url.set("https://joinup.ec.europa.eu/sites/default/files/custom-page/attachment/eupl_v1.2_en.pdf")
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
