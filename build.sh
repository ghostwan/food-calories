#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="8.7"
APP_ID="com.ghostwan.snapcal"
MAIN_ACTIVITY=".MainActivity"
BUILD_TYPE="debug"

# --- Helpers ---

red()   { printf '\033[0;31m%s\033[0m\n' "$*"; }
green() { printf '\033[0;32m%s\033[0m\n' "$*"; }
info()  { printf '=> %s\n' "$*"; }

usage() {
    cat <<EOF
Usage: $0 [-r] <command>

Options:
  -r        Use release build type instead of debug

Commands:
  build     Build the APK
  install   Build and install on connected device
  run       Build, install and launch on connected device
  clean     Clean build artifacts

Prerequisites:
  - JDK 17+ (java must be in PATH)
  - Android SDK (ANDROID_HOME or ANDROID_SDK_ROOT must be set,
    or SDK at ~/Library/Android/sdk or ~/Android/Sdk)
  - A connected device/emulator (for install/run)
EOF
    exit 1
}

check_java() {
    if ! command -v java &>/dev/null; then
        red "Error: Java not found. Install JDK 17+."
        exit 1
    fi
    info "Java: $(java -version 2>&1 | head -1)"
}

find_android_sdk() {
    if [ -n "${ANDROID_HOME:-}" ]; then
        SDK_ROOT="$ANDROID_HOME"
    elif [ -n "${ANDROID_SDK_ROOT:-}" ]; then
        SDK_ROOT="$ANDROID_SDK_ROOT"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        SDK_ROOT="$HOME/Library/Android/sdk"
    elif [ -d "$HOME/Android/Sdk" ]; then
        SDK_ROOT="$HOME/Android/Sdk"
    else
        red "Error: Android SDK not found."
        red "Set ANDROID_HOME or ANDROID_SDK_ROOT, or install the SDK."
        exit 1
    fi
    export ANDROID_HOME="$SDK_ROOT"
    info "Android SDK: $SDK_ROOT"

    # Update sdk.dir in local.properties (preserve other entries)
    if [ -f local.properties ]; then
        if grep -q '^sdk.dir=' local.properties; then
            sed -i.bak "s|^sdk.dir=.*|sdk.dir=$SDK_ROOT|" local.properties
            rm -f local.properties.bak
        else
            echo "sdk.dir=$SDK_ROOT" >> local.properties
        fi
    else
        echo "sdk.dir=$SDK_ROOT" > local.properties
    fi
}

setup_gradle() {
    GRADLE_DIR=".gradle-dist"
    GRADLE_BIN="$GRADLE_DIR/gradle-$GRADLE_VERSION/bin/gradle"

    if [ -f "./gradlew" ] && [ -f "./gradle/wrapper/gradle-wrapper.jar" ]; then
        info "Gradle wrapper already present."
        return
    fi

    if ! [ -f "$GRADLE_BIN" ]; then
        info "Downloading Gradle $GRADLE_VERSION..."
        mkdir -p "$GRADLE_DIR"
        curl -fsSL \
            "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
            -o "$GRADLE_DIR/gradle.zip"
        unzip -q "$GRADLE_DIR/gradle.zip" -d "$GRADLE_DIR"
        rm "$GRADLE_DIR/gradle.zip"
    fi

    info "Generating Gradle wrapper..."
    "$GRADLE_BIN" wrapper --gradle-version "$GRADLE_VERSION" --quiet
    green "Gradle wrapper generated."
}

find_adb() {
    if command -v adb &>/dev/null; then
        ADB="adb"
    elif [ -f "$SDK_ROOT/platform-tools/adb" ]; then
        ADB="$SDK_ROOT/platform-tools/adb"
    else
        red "Error: adb not found. Install Android platform-tools."
        exit 1
    fi
}

# --- Commands ---

do_build() {
    local task="assemble${BUILD_TYPE^}"
    info "Building $BUILD_TYPE APK..."
    ./gradlew "$task" --warning-mode=all
    APK="app/build/outputs/apk/$BUILD_TYPE/app-$BUILD_TYPE.apk"
    if [ -f "$APK" ]; then
        green "Build successful: $APK"
    else
        red "Build completed but APK not found at expected path."
        exit 1
    fi
}

do_install() {
    find_adb
    do_build
    info "Installing on device..."
    "$ADB" install -r "app/build/outputs/apk/$BUILD_TYPE/app-$BUILD_TYPE.apk"
    green "Installed successfully."
}

do_run() {
    do_install
    info "Launching app..."
    "$ADB" shell am start -n "$APP_ID/$MAIN_ACTIVITY"
    green "App launched."
}

do_clean() {
    info "Cleaning..."
    ./gradlew clean
    green "Clean complete."
}

# --- Main ---

cd "$(dirname "$0")"

while getopts "r" opt; do
    case "$opt" in
        r) BUILD_TYPE="release" ;;
        *) usage ;;
    esac
done
shift $((OPTIND - 1))

CMD="${1:-}"
[ -z "$CMD" ] && usage

check_java
find_android_sdk
setup_gradle

case "$CMD" in
    build)   do_build ;;
    install) do_install ;;
    run)     do_run ;;
    clean)   do_clean ;;
    *)       usage ;;
esac
