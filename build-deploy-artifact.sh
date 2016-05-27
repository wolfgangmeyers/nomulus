#!/bin/bash
bazel build --javacopt "-source 1.7" --javacopt "-target 1.7" //java/google/registry/env:production_ear
OK=$?
if [ $OK -eq 0 ]
then
 echo "Extracting ear file"
else
 echo "Build failed"
 exit 1
fi

rm -rf mercury-donuts
mkdir mercury-donuts
cd mercury-donuts
jar -xf ../bazel-genfiles/java/google/registry/env/production_ear.ear
cd ../
