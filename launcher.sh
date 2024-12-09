#!/usr/bin/bash

source ./env.sh
#source ./test_vars.sh

if [ ! -d "images" ]; then
  echo "Creating images directory"
  mkdir images
fi

./gradlew bootRun