@echo off
setlocal
set SCRIPT_DIR=%~dp0
if "%SCRIPT_DIR:~-1%"=="\" set SCRIPT_DIR=%SCRIPT_DIR:~0,-1%
set EXEC=%SCRIPT_DIR%\ModDevGradle-main\gradlew.bat
if not exist "%EXEC%" (
    echo ModDevGradle wrapper not found at %EXEC%
    exit /b 1
)
call "%EXEC%" -p "%SCRIPT_DIR%" %*
endlocal
