#!/usr/bin/env bash
# ---------------------------------------------------------------
# NOIRBOX Java application - compile and run (Linux / macOS / Git Bash)
# 1. Put mysql-connector-j-8.x.jar inside the lib folder
# 2. Edit config/db.properties with your MySQL password
# ---------------------------------------------------------------
set -e

cd "$(dirname "$0")"
mkdir -p out

echo "Compiling..."
find src -name "*.java" > sources.txt
javac -d out @sources.txt

echo "Running..."
java -cp "out:lib/*" noirbox.Main
