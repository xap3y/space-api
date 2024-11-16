#!/usr/bin/bash

source ./env.sh

if [ ! -d "images" ]; then
  echo "Creating images directory"
  mkdir images
fi

./gradlew bootRun