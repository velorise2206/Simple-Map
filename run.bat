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
echo      1) Fabric 26.3     (Java 25)
echo      2) NeoForge 26.3   (Java 25)
echo      3) Fabric 26.2     (Java 25)
echo      4) NeoForge 26.2   (Java 25)
echo      5) Fabric 26.1     (Java 25)
echo      6) NeoForge 26.1   (Java 25)
echo      7) Fabric 1.21.11  (Java 21)
echo      8) NeoForge 1.21.11(Java 21)
echo      9) Fabric 1.21.1   (Java 21)
echo     10) NeoForge 1.21.1 (Java 21)
echo     11) Forge 1.20.1    (Java 17)
echo     12) Fabric 1.20.1   (Java 17)
echo.
echo   [DEBUG ^& TELEMETRY CLIENT RUN]
echo     13) Fabric 26.3     (Debug)
echo     14) NeoForge 26.3   (Debug)
echo     15) Fabric 26.2     (Debug)
echo     16) NeoForge 26.2   (Debug)
echo     17) Fabric 26.1     (Debug)
echo     18) NeoForge 26.1   (Debug)
echo     19) Fabric 1.21.11  (Debug)
echo     20) NeoForge 1.21.11(Debug)
echo     21) Fabric 1.21.1   (Debug)
echo     22) NeoForge 1.21.1 (Debug)
echo     23) Forge 1.20.1    (Debug)
echo     24) Fabric 1.20.1   (Debug)
echo.
echo      0) Exit
echo ---------------------------------------------------------------------
set /p "CHOICE=Select an option [1-24]: "

:HANDLE_CHOICE
if "%CHOICE%"=="1" (
    call "%~dp0scripts\run-fabric.bat" 26.3
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="2" (
    call "%~dp0scripts\run-neoforge.bat" 26.3
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="3" (
    call "%~dp0scripts\run-fabric.bat" 26.2
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="4" (
    call "%~dp0scripts\run-neoforge.bat" 26.2
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="5" (
    call "%~dp0scripts\run-fabric.bat" 26.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="6" (
    call "%~dp0scripts\run-neoforge.bat" 26.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="7" (
    call "%~dp0scripts\run-fabric.bat" 1.21.11
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="8" (
    call "%~dp0scripts\run-neoforge.bat" 1.21.11
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="9" (
    call "%~dp0scripts\run-fabric.bat" 1.21.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="10" (
    call "%~dp0scripts\run-neoforge.bat" 1.21.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="11" (
    call "%~dp0scripts\run-forge-1.20.1.bat" 1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="12" (
    call "%~dp0scripts\run-fabric.bat" 1.20.1
    exit /b %ERRORLEVEL%
)

if "%CHOICE%"=="13" (
    call "%~dp0scripts\run-fabric-26.3-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="14" (
    call "%~dp0scripts\run-neoforge-26.3-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="15" (
    call "%~dp0scripts\run-fabric-26.2-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="16" (
    call "%~dp0scripts\run-neoforge-26.2-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="17" (
    call "%~dp0scripts\run-fabric-26.1-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="18" (
    call "%~dp0scripts\run-neoforge-26.1-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="19" (
    call "%~dp0scripts\run-fabric-1.21.11-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="20" (
    call "%~dp0scripts\run-neoforge-1.21.11-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="21" (
    call "%~dp0scripts\run-fabric-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="22" (
    call "%~dp0scripts\run-neoforge-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="23" (
    call "%~dp0scripts\run-forge-1.20.1-debug.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="24" (
    call "%~dp0scripts\run-fabric-1.20.1-debug.bat"
    exit /b %ERRORLEVEL%
)

if "%CHOICE%"=="0" exit /b 0

echo Invalid selection: "%CHOICE%"
timeout /t 2 >nul
goto :MENU
