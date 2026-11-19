#!/usr/bin/env bash
# NOIRBOX web API - the Java <-> MySQL backend for the website.
set -e

cd "$(dirname "$0")"
mkdir -p out

echo "Compiling..."
find src -name "*.java" > sources.txt
javac -d out @sources.txt

echo "Starting API on http://localhost:8080 ..."
java -cp "out:lib/*" noirbox.api.ApiMain "$@"
