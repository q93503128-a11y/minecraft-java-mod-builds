@echo off
setlocal

set "GRADLE_HOME=%LOCALAPPDATA%\VillageGuardians\gradle-bootstrap\gradle-9.2.1"

where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java 25 is required but java.exe was not found.
  exit /b 1
)

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  echo [ERROR] Gradle 9.2.1 is not installed at "%GRADLE_HOME%".
  echo Install the official Gradle 9.2.1 binary distribution there, then retry.
  exit /b 1
)

java -version
set "GRADLE_OPTS=%GRADLE_OPTS% -Djavax.net.ssl.trustStoreType=Windows-ROOT"
call "%GRADLE_HOME%\bin\gradle.bat" --no-daemon --no-configuration-cache clean build --stacktrace --console=plain
if errorlevel 1 exit /b 1

echo.
echo [SUCCESS] JAR output:
dir /b "%~dp0build\libs\*.jar"
