@echo off
setlocal enabledelayedexpansion
set "REPO_ROOT=%~dp0.."
cd /d "%REPO_ROOT%"

title Simple Map - NeoForge 1.21.1 Client (DEBUG ^& TELEMETRY Mode)

echo =====================================================================
echo          SIMPLE MAP - NEOFORGE 1.21.1 DEBUG ^& TELEMETRY LAUNCHER
echo =====================================================================
echo.
echo [1] DEBUG MODE ENABLED ^& TELEMETRY:
echo   - JVM Flag: -Dsimplemap.debug=true enabled.
echo   - MapDebugRecorder is active and ready.
echo.
echo [2] IN-GAME HOTKEYS (WHILE PLAYING OR IN FULLSCREEN MAP 'M'):
echo   - [ F8  ] : TOGGLE HUD OVERLAY (displays live map metrics:
echo              FPS, frame time, Heap/GC, Page states, FBO status on screen).
echo   - [ F9  ] : TOGGLE METRICS RECORDING (default: auto-enabled on world join).
echo   - [ F10 ] : *** MARK DEBUG ISSUE ***:
echo              - Press F10 immediately when noticing map pan stutter/lag!
echo              - Press F10 when panning over black/unloaded cave regions!
echo              - Pressing F10 triggers Burst Mode (250ms sampling for 20s)
echo                and records a USER_MARKER event for root-cause analysis.
echo.
echo [3] METRICS LOG LOCATION:
echo   - Directory: 1.21.1\neoforge\run\simplemap-debug\
echo   - Files: metrics.csv (quantitative metrics) and events.jsonl (events/errors)
echo =====================================================================
echo.

:: Detect JDK 21
if defined JAVA21_HOME (
    if exist "%JAVA21_HOME%\bin\java.exe" (
        set "JAVA_HOME=%JAVA21_HOME%"
        goto :JAVA_DETECTED
    )
)

if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
    goto :JAVA_DETECTED
)

if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    goto :JAVA_DETECTED
)

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        goto :JAVA_DETECTED
    )
)

:JAVA_DETECTED
if defined JAVA_HOME (
    echo [Environment] Using JDK 21 at: "%JAVA_HOME%"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
) else (
    echo [Warning] JAVA_HOME not found; using default Java from PATH.
)

:: Enable debug property environment variable
set "SIMPLEMAP_DEBUG=true"

echo [Launcher] Launching NeoForge 1.21.1 Client in Debug mode...
echo ---------------------------------------------------------------------

cd /d "%REPO_ROOT%\1.21.1\neoforge"
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

set "LATEST_LOG_TXT=%REPO_ROOT%\1.21.1\neoforge\run\simplemap-debug\latest.txt"
set "LATEST_DIR="
if exist "%LATEST_LOG_TXT%" (
    for /f "usebackq delims=" %%i in ("%LATEST_LOG_TXT%") do set "LATEST_DIR=%%i"
)
if defined LATEST_DIR (
    echo.
    echo [Telemetry Output] Latest session telemetry directory:
    echo   !LATEST_DIR!
    echo.
    echo   Recorded files:
    if exist "!LATEST_DIR!\metrics.csv" (
        echo   [x] !LATEST_DIR!\metrics.csv
    )
    if exist "!LATEST_DIR!\events.jsonl" (
        echo   [x] !LATEST_DIR!\events.jsonl
    )
    echo.
    set /p "OPEN_DIR=Open the generated debug folder? [Y/N]: "
    if /i "!OPEN_DIR!"=="Y" (
        start "" "!LATEST_DIR!"
    )
) else (
    echo [Telemetry Output] No recent debug folder found - you may not have joined a world.
)

echo.
pause
exit /b %EXITCODE%

