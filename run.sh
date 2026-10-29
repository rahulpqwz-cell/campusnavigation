#!/bin/sh
# Compile and start the CampusNav Swing application.
cd "$(dirname "$0")" || exit 1
mkdir -p out
find src -name '*.java' > sources.txt
javac -encoding UTF-8 -d out @sources.txt || exit 1
rm -f sources.txt
java -cp out campusnav.Main
