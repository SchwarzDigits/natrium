#!/usr/bin/env bash

# Pre-analyzer prep for natrium.
# 
# 1. Makes JDK 17 available to the ORT container. natrium / Kalium request
# JDK 17 via `kotlin { jvmToolchain(17) }`. The ORT container only ships
# JDK 21, so without this prep Gradle calls foojay-resolver to download
# one, and the foojay plugin baked into Gradle 9.2.1 crashes on
# `JvmVendorSpec.IBM_SEMERU` (API mismatch).
# 
# Workaround: copy the JDK 17 from the GitHub runner host into $HOME
# (which the ORT container bind-mounts), point Gradle at it via
# gradle.properties, and disable auto-download so foojay is never
# invoked.

set -euxo pipefail

# --- 1. JDK 17 for the ORT container --------------------------

# GitHub-hosted runners pre-install Temurin JDKs at /usr/lib/jvm/.
# The ORT container only mounts $HOME, so copy JDK 17 into $HOME first.

RUNNER_JDK17=/usr/lib/jvm/temurin-17-jdk-amd64
CONTAINER_JDK17=$HOME/.jdks/temurin-17
mkdir -p "$HOME/.jdks"
cp -a "$RUNNER_JDK17" "$CONTAINER_JDK17"

# Tell Gradle where to find the toolchain and disable auto-download
# (auto-download triggers the foojay-resolver crash).

mkdir -p "$HOME/.gradle"
cat >> "$HOME/.gradle/gradle.properties" <<EOF
org.gradle.java.installations.paths=$CONTAINER_JDK17
org.gradle.java.installations.auto-download=false
EOF

echo "Gradle toolchain config:"
cat "$HOME/.gradle/gradle.properties"
