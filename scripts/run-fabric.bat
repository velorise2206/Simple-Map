@echo off
setlocal
set "REPO_ROOT=%~dp0.."
cd /d "%REPO_ROOT%"

set "CHOICE=%~1"
if not "%CHOICE%"=="" goto :HANDLE_CHOICE

:MENU
cls
echo ====================================================
echo         Simple Map - Fabric Dev Client
echo ====================================================
echo   1) Fabric 26.2    (Java 25) [Normal]
echo   2) Fabric 26.1    (Java 25) [Normal]
echo   3) Fabric 1.21.11 (Java 21) [Normal]
echo   4) Fabric 1.21.1  (Java 21) [Normal]
echo   5) Fabric 1.20.1  (Java 17) [Normal]
echo   6) Fabric 26.2    (Java 25) [DEBUG ^& TELEMETRY]
echo   7) Fabric 26.1    (Java 25) [DEBUG ^& TELEMETRY]
echo   8) Fabric 1.21.11 (Java 21) [DEBUG ^& TELEMETRY]
echo   9) Fabric 1.21.1  (Java 21) [DEBUG ^& TELEMETRY]
echo  10) Fabric 1.20.1  (Java 17) [DEBUG ^& TELEMETRY]
echo   0) Exit
echo ----------------------------------------------------
set /p "CHOICE=Select version to run [1-10]: "

:HANDLE_CHOICE
if "%CHOICE%"=="1" goto :RUN_262
if "%CHOICE%"=="26.2" goto :RUN_262
if "%CHOICE%"=="262" goto :RUN_262

if "%CHOICE%"=="2" goto :RUN_261
if "%CHOICE%"=="26.1" goto :RUN_261
if "%CHOICE%"=="261" goto :RUN_261

if "%CHOICE%"=="3" goto :RUN_12111
if "%CHOICE%"=="1.21.11" goto :RUN_12111
if "%CHOICE%"=="12111" goto :RUN_12111

if "%CHOICE%"=="4" goto :RUN_1211
if "%CHOICE%"=="1.21.1" goto :RUN_1211
if "%CHOICE%"=="1211" goto :RUN_1211

if "%CHOICE%"=="5" goto :RUN_1201
if "%CHOICE%"=="1.20.1" goto :RUN_1201
if "%CHOICE%"=="1201" goto :RUN_1201

if "%CHOICE%"=="6" goto :RUN_DEBUG_262
if "%CHOICE%"=="7" goto :RUN_DEBUG_261
if "%CHOICE%"=="8" goto :RUN_DEBUG_12111
if "%CHOICE%"=="9" goto :RUN_DEBUG_1211
if /i "%CHOICE%"=="debug" goto :RUN_DEBUG_1211
if "%CHOICE%"=="d" goto :RUN_DEBUG_1211
if "%CHOICE%"=="10" goto :RUN_DEBUG_1201

if "%CHOICE%"=="0" exit /b 0

echo Invalid selection: "%CHOICE%"
timeout /t 2 >nul
goto :MENU

:RUN_DEBUG_262
call "%~dp0run-fabric-26.2-debug.bat"
exit /b %ERRORLEVEL%

:RUN_DEBUG_261
call "%~dp0run-fabric-26.1-debug.bat"
exit /b %ERRORLEVEL%

:RUN_DEBUG_12111
call "%~dp0run-fabric-1.21.11-debug.bat"
exit /b %ERRORLEVEL%

:RUN_DEBUG_1211
call "%~dp0run-fabric-debug.bat"
exit /b %ERRORLEVEL%

:RUN_DEBUG_1201
call "%~dp0run-fabric-1.20.1-debug.bat"
exit /b %ERRORLEVEL%

:RUN_262
set "REQ_JAVA=25"
set "TARGET_DIR=26.2\fabric"
goto :START_CLIENT

:RUN_261
set "REQ_JAVA=25"
set "TARGET_DIR=26.1\fabric"
goto :START_CLIENT

:RUN_12111
set "REQ_JAVA=21"
set "TARGET_DIR=1.21.11\fabric"
goto :START_CLIENT

:RUN_1211
set "REQ_JAVA=21"
set "TARGET_DIR=1.21.1\fabric"
goto :START_CLIENT

:RUN_1201
set "REQ_JAVA=17"
set "TARGET_DIR=1.20.1\fabric"
goto :START_CLIENT

:START_CLIENT
call "%~dp0find-jdk.bat" %REQ_JAVA%
if errorlevel 1 (
    pause
    exit /b 1
)
echo [Simple Map] Launching %TARGET_DIR% Fabric client...
cd /d "%REPO_ROOT%\%TARGET_DIR%"
call gradlew.bat runClient
set "EXITCODE=%ERRORLEVEL%"
cd /d "%REPO_ROOT%"
if not "%EXITCODE%"=="0" (
    echo.
    echo Fabric client run failed with code %EXITCODE%.
    pause
)
exit /b %EXITCODE%
