#!/usr/bin/env sh
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out src/HospitalServer.java
java -cp out HospitalServer
