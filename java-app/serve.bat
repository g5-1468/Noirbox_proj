@echo off
REM ---------------------------------------------------------------
REM NOIRBOX web API - the Java <-> MySQL backend for the website.
REM Keep this window open while you use the site.
REM ---------------------------------------------------------------

cd /d "%~dp0"

if not exist out mkdir out

echo Compiling...
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
if errorlevel 1 (
    echo.
    echo Compilation failed. Check the errors above.
    pause
    exit /b 1
)

echo Starting API on http://localhost:8080 ...
java -cp "out;lib/*" noirbox.api.ApiMain %*

pause
