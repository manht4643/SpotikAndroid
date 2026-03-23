#!/bin/bash
set -e

cd /opt/spotik/server

# Install Gradle if not present
if ! command -v gradle &> /dev/null; then
    echo "=== Installing Gradle ==="
    apt-get install -y -qq zip unzip 2>/dev/null
    curl -sL "https://services.gradle.org/distributions/gradle-8.11-bin.zip" -o /tmp/gradle.zip
    unzip -q -o /tmp/gradle.zip -d /opt
    ln -sf /opt/gradle-8.11/bin/gradle /usr/local/bin/gradle
    rm /tmp/gradle.zip
fi

echo "=== Gradle version ==="
gradle --version | head -3

echo "=== Building fat JAR ==="
gradle buildFatJar --no-daemon 2>&1 | tail -20

echo "=== JAR built ==="
ls -la build/libs/*.jar 2>/dev/null || echo "NO JAR FOUND"

