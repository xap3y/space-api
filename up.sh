#!/bin/bash
./gradlew build -x test

VERSION=$(grep -oP "version\s*=\s*'\K[^']+" build.gradle)

echo "Copying build: space-$VERSION.jar"

JAR_FILE='./build/libs/space-'$VERSION'.jar'

scp -i ~/.ssh/vps_sky "$JAR_FILE" xap3y@internal.sky.xap3y.eu:/home/xap3y/space/
cp "$JAR_FILE" /usr/share/cdn/artifacts/
