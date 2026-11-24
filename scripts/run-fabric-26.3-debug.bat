@echo off
setlocal enabledelayedexpansion
set "REPO_ROOT=%~dp0.."
cd /d "%REPO_ROOT%"

title Simple Map - Fabric 26.3 Client (DEBUG ^& TELEMETRY Mode)

echo =====================================================================
echo       SIMPLE MAP - FABRIC 26.3 DEBUG ^& TELEMETRY LAUNCHER
echo =====================================================================
echo   - JVM Flag: -Dsimplemap.debug=true enabled.
echo   - Hotkeys: [F8] HUD Overlay, [F9] Toggle Recording, [F10] Mark Issue
echo   - Logs: 26.3\fabric\run\simplemap-debug\
echo =====================================================================
echo.

call "%~dp0find-jdk.bat" 25
if errorlevel 1 (
    pause
    exit /b 1
)

set "SIMPLEMAP_DEBUG=true"

echo [Launcher] Launching Fabric 26.3 Client in Debug mode...
echo ---------------------------------------------------------------------

cd /d "%REPO_ROOT%\26.3\fabric"
call gradlew.bat runClient -Psimplemap.debug=true "-Dsimplemap.debug=true"
set "EXITCODE=%ERRORLEVEL%"
cd /d "%REPO_ROOT%"

echo.
echo =====================================================================
if "%EXITCODE%"=="0" (
    echo [Launcher] Game closed normally.
) else (
    echo [Launcher] Game exited with code: %EXITCODE%
)
echo =====================================================================

set "LATEST_LOG_TXT=%REPO_ROOT%\26.3\fabric\run\simplemap-debug\latest.txt"
set "LATEST_DIR="
if exist "%LATEST_LOG_TXT%" (
    for /f "usebackq delims=" %%i in ("%LATEST_LOG_TXT%") do set "LATEST_DIR=%%i"
)
if defined LATEST_DIR (
    echo [Telemetry Output] Latest session directory: !LATEST_DIR!
) else (
    echo [Telemetry Output] No recent debug folder found.
)

exit /b %EXITCODE%
