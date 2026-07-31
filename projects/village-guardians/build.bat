@echo off
setlocal enabledelayedexpansion

set "GRADLE_VERSION=9.2.1"
set "BOOTSTRAP_DIR=%~dp0.gradle-bootstrap"
set "GRADLE_HOME=%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%-bin.zip"

where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java 25 is required but java.exe was not found.
  exit /b 1
)

java -version

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  echo [INFO] Downloading Gradle %GRADLE_VERSION%...
  if not exist "%BOOTSTRAP_DIR%" mkdir "%BOOTSTRAP_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_ZIP%'; Expand-Archive -Path '%GRADLE_ZIP%' -DestinationPath '%BOOTSTRAP_DIR%' -Force"
  if errorlevel 1 exit /b 1
)

call "%GRADLE_HOME%\bin\gradle.bat" --no-daemon clean build --stacktrace
if errorlevel 1 exit /b 1

echo.
echo [SUCCESS] JAR output:
dir /b "%~dp0build\libs\*.jar"
