@echo off
title Kingston Campus 3D - Launching...

:: Check Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH.
    echo Please install Java 21 from: https://adoptium.net/
    pause
    exit /b 1
)

:: Launch the game
echo Starting Kingston Campus 3D...
java -Xms128m -Xmx2048m -XX:+UseG1GC -jar Kingston-Campus-3D-Game-all.jar

if %errorlevel% neq 0 (
    echo.
    echo Game exited with an error. Press any key to close.
    pause
)
