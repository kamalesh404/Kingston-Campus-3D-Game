#!/bin/bash
echo "Kingston Campus 3D - Launching..."

# Check Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed."
    echo "Install Java 21 from: https://adoptium.net/"
    exit 1
fi

java -Xms128m -Xmx2048m -XX:+UseG1GC -jar Kingston-Campus-3D-Game-all.jar
