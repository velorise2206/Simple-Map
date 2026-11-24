@echo off
setlocal
set "REPO_ROOT=%~dp0.."
cd /d "%REPO_ROOT%"

set "CHOICE=%~1"
if not "%CHOICE%"=="" goto :HANDLE_CHOICE

:MENU
cls
echo ====================================================
echo         Simple Map - NeoForge Dev Client
echo ====================================================
echo   1) NeoForge 26.3    (Java 25) [Normal]
echo   2) NeoForge 26.2    (Java 25) [Normal]
echo   3) NeoForge 26.1    (Java 25) [Normal]
echo   4) NeoForge 1.21.11 (Java 21) [Normal]
echo   5) NeoForge 1.21.1  (Java 21) [Normal]
echo   6) NeoForge 26.3    (Java 25) [DEBUG ^& TELEMETRY]
echo   7) NeoForge 26.2    (Java 25) [DEBUG ^& TELEMETRY]
echo   8) NeoForge 26.1    (Java 25) [DEBUG ^& TELEMETRY]
echo   9) NeoForge 1.21.11 (Java 21) [DEBUG ^& TELEMETRY]
echo  10) NeoForge 1.21.1  (Java 21) [DEBUG ^& TELEMETRY]
echo   0) Exit
echo ----------------------------------------------------
set /p "CHOICE=Select version to run [1-10]: "

:HANDLE_CHOICE
if "%CHOICE%"=="1" goto :RUN_263
if "%CHOICE%"=="26.3" goto :RUN_263
if "%CHOICE%"=="263" goto :RUN_263

if "%CHOICE%"=="2" goto :RUN_262
if "%CHOICE%"=="26.2" goto :RUN_262
if "%CHOICE%"=="262" goto :RUN_262

if "%CHOICE%"=="3" goto :RUN_261
if "%CHOICE%"=="26.1" goto :RUN_261
if "%CHOICE%"=="261" goto :RUN_261

if "%CHOICE%"=="4" goto :RUN_12111
if "%CHOICE%"=="1.21.11" goto :RUN_12111
if "%CHOICE%"=="12111" goto :RUN_12111

if "%CHOICE%"=="5" goto :RUN_1211
if "%CHOICE%"=="1.21.1" goto :RUN_1211
if "%CHOICE%"=="1211" goto :RUN_1211

if "%CHOICE%"=="6" goto :RUN_DEBUG_263
if "%CHOICE%"=="7" goto :RUN_DEBUG_262
if "%CHOICE%"=="8" goto :RUN_DEBUG_261
if "%CHOICE%"=="9" goto :RUN_DEBUG_12111
if "%CHOICE%"=="10" goto :RUN_DEBUG_1211
if /i "%CHOICE%"=="debug" goto :RUN_DEBUG_1211
if /i "%CHOICE%"=="-debug" goto :RUN_DEBUG_1211
if /i "%CHOICE%"=="--debug" goto :RUN_DEBUG_1211
if /i "%CHOICE%"=="d" goto :RUN_DEBUG_1211

if "%CHOICE%"=="0" exit /b 0

echo Invalid selection: "%CHOICE%"
timeout /t 2 >nul
goto :MENU

:RUN_DEBUG_263
call "%~dp0run-neoforge-26.3-debug.bat"
exit /b %ERRORLEVEL%

:RUN_DEBUG_262
call "%~dp0run-neoforge-26.2-debug.bat"
exit /b %ERRORLEVEL%

:RUN_DEBUG_261
call "%~dp0run-neoforge-26.1-debug.bat"
exit /b %ERRORLEVEL%

:RUN_DEBUG_12111
call "%~dp0run-neoforge-1.21.11-debug.bat"
exit /b %ERRORLEVEL%

:RUN_DEBUG_1211
call "%~dp0run-neoforge-debug.bat"
exit /b %ERRORLEVEL%

:RUN_263
set "REQ_JAVA=25"
set "TARGET_DIR=26.3\neoforge"
goto :START_CLIENT

:RUN_262
set "REQ_JAVA=25"
set "TARGET_DIR=26.2\neoforge"
goto :START_CLIENT

:RUN_261
set "REQ_JAVA=25"
set "TARGET_DIR=26.1\neoforge"
goto :START_CLIENT

:RUN_12111
set "REQ_JAVA=21"
set "TARGET_DIR=1.21.11\neoforge"
goto :START_CLIENT

:RUN_1211
set "REQ_JAVA=21"
set "TARGET_DIR=1.21.1\neoforge"
goto :START_CLIENT

:START_CLIENT
call "%~dp0find-jdk.bat" %REQ_JAVA%
if errorlevel 1 (
    pause
    exit /b 1
)
echo [Simple Map] Launching %TARGET_DIR% NeoForge client...
cd /d "%REPO_ROOT%\%TARGET_DIR%"
call gradlew.bat runClient
set "EXITCODE=%ERRORLEVEL%"
cd /d "%REPO_ROOT%"
if not "%EXITCODE%"=="0" (
    echo.
    echo NeoForge client run failed with code %EXITCODE%.
    pause
)
exit /b %EXITCODE%
