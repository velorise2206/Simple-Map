@echo off
setlocal
cd /d "%~dp0"

set "CHOICE=%~1"
if not "%CHOICE%"=="" goto :HANDLE_CHOICE

:MENU
cls
echo =====================================================================
echo                VELORISE SIMPLE MAP - CLIENT LAUNCHER
echo =====================================================================
echo   [NORMAL CLIENT RUN]
echo      1) Fabric 26.1     (Java 25)
echo      2) NeoForge 26.1   (Java 25)
echo      3) Fabric 1.21.11  (Java 21)
echo      4) NeoForge 1.21.11(Java 21)
echo      5) Fabric 1.21.1   (Java 21)
echo      6) NeoForge 1.21.1 (Java 21)
echo      7) Forge 1.20.1    (Java 17)
echo      8) Fabric 1.20.1   (Java 17)
echo.
echo   [DEBUG ^& TELEMETRY CLIENT RUN]
echo      9) Fabric 26.1     (Debug)
echo     10) NeoForge 26.1   (Debug)
echo     11) Fabric 1.21.11  (Debug)
echo     12) NeoForge 1.21.11(Debug)
echo     13) Fabric 1.21.1   (Debug)
echo     14) NeoForge 1.21.1 (Debug)
echo     15) Forge 1.20.1    (Debug)
echo     16) Fabric 1.20.1   (Debug)
echo.
echo      0) Exit
echo ---------------------------------------------------------------------
set /p "CHOICE=Select an option [1-16]: "

:HANDLE_CHOICE
if "%CHOICE%"=="1" (
    call "%~dp0scripts\run-fabric.bat" 26.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="2" (
    call "%~dp0scripts\run-neoforge.bat" 26.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="3" (
    call "%~dp0scripts\run-fabric.bat" 1.21.11
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="4" (
    call "%~dp0scripts\run-neoforge.bat" 1.21.11
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="5" (
    call "%~dp0scripts\run-fabric.bat" 1.21.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="6" (
    call "%~dp0scripts\run-neoforge.bat" 1.21.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="7" (
    call "%~dp0scripts\run-forge-1.20.1.bat" 1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="8" (
    call "%~dp0scripts\run-fabric.bat" 1.20.1
    exit /b %ERRORLEVEL%
)

if "%CHOICE%"=="9" (
    call "%~dp0scripts\run-fabric-26.1-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="10" (
    call "%~dp0scripts\run-neoforge-26.1-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="11" (
    call "%~dp0scripts\run-fabric-1.21.11-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="12" (
    call "%~dp0scripts\run-neoforge-1.21.11-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="13" (
    call "%~dp0scripts\run-fabric-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="14" (
    call "%~dp0scripts\run-neoforge-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="15" (
    call "%~dp0scripts\run-forge-1.20.1-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="16" (
    call "%~dp0scripts\run-fabric-1.20.1-debug.bat"
    exit /b %ERRORLEVEL%
)

if "%CHOICE%"=="0" exit /b 0

echo Invalid selection: "%CHOICE%"
timeout /t 2 >nul
goto :MENU
