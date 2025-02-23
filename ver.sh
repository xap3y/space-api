#!/bin/bash

read -p "New version: " NEW_VERSION

BUILD_GRADLE_PATH="./build.gradle"
SPACE_APP_PATH="./src/main/java/me/xap3y/space/SpaceApplication.java"

sed -i "s/version = '[^']*'/version = '$NEW_VERSION'/" "$BUILD_GRADLE_PATH"
sed -i "s/public static final String VERSION = \"[^\"]*\"/public static final String VERSION = \"$NEW_VERSION\"/" "$SPACE_APP_PATH"

echo "Version updated to $NEW_VERSION in gradle and main class."
