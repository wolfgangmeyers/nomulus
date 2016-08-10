#!/bin/bash

if [ -z "$1" ]; then
  echo ".ear directory required"
  exit 1
fi
EAR_DIR=$1

if ! bazel build --javacopt "-source 1.7" --javacopt "-target 1.7" //java/google/registry:registry.ear; then
  echo "Build failed"
  exit 1
fi

echo "Extracting .ear file to" $PWD/$EAR_DIR
rm -rf $EAR_DIR
mkdir $EAR_DIR
cd $EAR_DIR
jar -xf ../bazel-genfiles/java/google/registry/registry.ear
cd $OLDPWD
