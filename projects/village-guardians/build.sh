#!/usr/bin/env sh
set -eu

GRADLE_VERSION=9.2.1
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BOOTSTRAP_DIR="$SCRIPT_DIR/.gradle-bootstrap"
GRADLE_HOME="$BOOTSTRAP_DIR/gradle-$GRADLE_VERSION"
GRADLE_ZIP="$BOOTSTRAP_DIR/gradle-$GRADLE_VERSION-bin.zip"

if ! command -v java >/dev/null 2>&1; then
  echo "[ERROR] Java 25 is required but java was not found." >&2
  exit 1
fi

java -version

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$BOOTSTRAP_DIR"
  echo "[INFO] Downloading Gradle $GRADLE_VERSION..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$GRADLE_ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$GRADLE_ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  else
    echo "[ERROR] curl or wget is required to bootstrap Gradle." >&2
    exit 1
  fi
  unzip -q -o "$GRADLE_ZIP" -d "$BOOTSTRAP_DIR"
fi

cd "$SCRIPT_DIR"
"$GRADLE_HOME/bin/gradle" --no-daemon clean build --stacktrace
printf '\n[SUCCESS] JAR output:\n'
find "$SCRIPT_DIR/build/libs" -maxdepth 1 -type f -name '*.jar' -print
