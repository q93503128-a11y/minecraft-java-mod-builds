@echo off
setlocal
cd /d "%~dp0\.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0m0-preflight.ps1"
set EXITCODE=%ERRORLEVEL%
echo.
if not "%EXITCODE%"=="0" (
  echo M0 preflight did not pass. See local\m0\report\M0_LOCAL_FINGERPRINTS.json
) else (
  echo M0 fingerprint audit passed. Runtime smoke test is still required.
)
exit /b %EXITCODE%
