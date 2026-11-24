@echo off
setlocal
set "REPO_ROOT=%~dp0.."
cd /d "%REPO_ROOT%"

set "CHOICE=%~1"
if not "%CHOICE%"=="" goto :HANDLE_CHOICE

:MENU
cls
echo =====================================================================
echo         SIMPLE MAP - ALL LOADERS DEBUG ^& TELEMETRY LAUNCHER
echo =====================================================================
echo   1) Fabric 26.2     (Java 25) [DEBUG ^& TELEMETRY]
echo   2) NeoForge 26.2   (Java 25) [DEBUG ^& TELEMETRY]
echo   3) Fabric 26.1     (Java 25) [DEBUG ^& TELEMETRY]
echo   4) NeoForge 26.1   (Java 25) [DEBUG ^& TELEMETRY]
echo   5) Fabric 1.21.11  (Java 21) [DEBUG ^& TELEMETRY]
echo   6) NeoForge 1.21.11(Java 21) [DEBUG ^& TELEMETRY]
echo   7) Fabric 1.21.1   (Java 21) [DEBUG ^& TELEMETRY]
echo   8) NeoForge 1.21.1 (Java 21) [DEBUG ^& TELEMETRY]
echo   9) Forge 1.20.1    (Java 17) [DEBUG ^& TELEMETRY]
echo  10) Fabric 1.20.1   (Java 17) [DEBUG ^& TELEMETRY]
echo   0) Exit
echo ---------------------------------------------------------------------
echo   In-game hotkeys: F8 (HUD Overlay), F9 (Toggle Log), F10 (Mark Issue)
echo =====================================================================
set /p "CHOICE=Select version to launch in debug mode [1-10]: "

:HANDLE_CHOICE
if "%CHOICE%"=="1" goto :RUN_FABRIC_262
if "%CHOICE%"=="26.2" goto :RUN_FABRIC_262
if "%CHOICE%"=="2" goto :RUN_NEOFORGE_262
if "%CHOICE%"=="3" goto :RUN_FABRIC_261
if "%CHOICE%"=="26.1" goto :RUN_FABRIC_261
if "%CHOICE%"=="4" goto :RUN_NEOFORGE_261
if "%CHOICE%"=="5" goto :RUN_FABRIC_12111
if "%CHOICE%"=="1.21.11" goto :RUN_FABRIC_12111
if "%CHOICE%"=="6" goto :RUN_NEOFORGE_12111
if "%CHOICE%"=="7" goto :RUN_FABRIC_1211
if "%CHOICE%"=="1.21.1" goto :RUN_FABRIC_1211
if "%CHOICE%"=="fabric" goto :RUN_FABRIC_1211
if "%CHOICE%"=="8" goto :RUN_NEOFORGE_1211
if "%CHOICE%"=="neoforge" goto :RUN_NEOFORGE_1211
if "%CHOICE%"=="neo" goto :RUN_NEOFORGE_1211
if "%CHOICE%"=="9" goto :RUN_FORGE_1201
if "%CHOICE%"=="forge" goto :RUN_FORGE_1201
if "%CHOICE%"=="1.20.1" goto :RUN_FORGE_1201
if "%CHOICE%"=="10" goto :RUN_FABRIC_1201
if "%CHOICE%"=="1.20.1\fabric" goto :RUN_FABRIC_1201
if "%CHOICE%"=="0" exit /b 0

echo Invalid selection: "%CHOICE%"
timeout /t 2 >nul
goto :MENU

:RUN_FABRIC_262
call "%~dp0run-fabric-26.2-debug.bat"
exit /b %ERRORLEVEL%

:RUN_NEOFORGE_262
call "%~dp0run-neoforge-26.2-debug.bat"
exit /b %ERRORLEVEL%

:RUN_FABRIC_261
call "%~dp0run-fabric-26.1-debug.bat"
exit /b %ERRORLEVEL%

:RUN_NEOFORGE_261
call "%~dp0run-neoforge-26.1-debug.bat"
exit /b %ERRORLEVEL%

:RUN_FABRIC_12111
call "%~dp0run-fabric-1.21.11-debug.bat"
exit /b %ERRORLEVEL%

:RUN_NEOFORGE_12111
call "%~dp0run-neoforge-1.21.11-debug.bat"
exit /b %ERRORLEVEL%

:RUN_FABRIC_1211
call "%~dp0run-fabric-debug.bat"
exit /b %ERRORLEVEL%

:RUN_NEOFORGE_1211
call "%~dp0run-neoforge-debug.bat"
exit /b %ERRORLEVEL%

:RUN_FORGE_1201
call "%~dp0run-forge-1.20.1-debug.bat"
exit /b %ERRORLEVEL%

:RUN_FABRIC_1201
call "%~dp0run-fabric-1.20.1-debug.bat"
exit /b %ERRORLEVEL%
