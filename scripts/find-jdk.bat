@echo off
set "REQ_VER=%~1"
set "FOUND_JDK="

if "%REQ_VER%"=="25" if defined JAVA25_HOME if exist "%JAVA25_HOME%\bin\java.exe" set "FOUND_JDK=%JAVA25_HOME%"
if "%REQ_VER%"=="21" if defined JAVA21_HOME if exist "%JAVA21_HOME%\bin\java.exe" set "FOUND_JDK=%JAVA21_HOME%"
if "%REQ_VER%"=="17" if defined JAVA17_HOME if exist "%JAVA17_HOME%\bin\java.exe" set "FOUND_JDK=%JAVA17_HOME%"

if defined FOUND_JDK goto :APPLY

for /d %%D in (
    "C:\Program Files\Eclipse Adoptium\jdk-%REQ_VER%*"
    "C:\Program Files\Java\jdk-%REQ_VER%*"
    "C:\Program Files\Microsoft\jdk-%REQ_VER%*"
    "C:\Program Files\Zulu\zulu-%REQ_VER%*"
    "C:\Program Files\Amazon Corretto\jdk%REQ_VER%*"
    "%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-%REQ_VER%*"
    "%USERPROFILE%\.jdks\*%REQ_VER%*"
) do (
    if not defined FOUND_JDK if exist "%%~D\bin\java.exe" set "FOUND_JDK=%%~D"
)

:APPLY
if defined FOUND_JDK (
    set "JAVA_HOME=%FOUND_JDK%"
    set "PATH=%FOUND_JDK%\bin;%PATH%"
    echo [Environment] Using JDK %REQ_VER% at: "%FOUND_JDK%"
    exit /b 0
)

echo [Error] Could not find JDK %REQ_VER% automatically.
echo         Please install JDK %REQ_VER% or set JAVA%REQ_VER%_HOME.
exit /b 1
