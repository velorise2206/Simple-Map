@echo off
setlocal
set "ROOT=%~dp0"
set "OUT=%ROOT%..\mods\simplemap"
if not exist "%OUT%" mkdir "%OUT%"

call "%ROOT%scripts\find-jdk.bat" 17
if errorlevel 1 exit /b 1

pushd "%ROOT%paper"
call gradlew.bat build --stacktrace --no-daemon --console=plain
if errorlevel 1 (
    popd
    exit /b %ERRORLEVEL%
)
for %%F in (build\libs\simple-map-paper-*.jar) do (
    copy /Y "%%F" "%OUT%\" >nul
    echo [OK] Copied %%~nxF -^> %OUT%
)
popd
endlocal
