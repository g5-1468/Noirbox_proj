@echo off
REM ---------------------------------------------------------------
REM NOIRBOX Java application - compile and run (Windows)
REM 1. Put mysql-connector-j-8.x.jar inside the lib folder
REM 2. Edit config\db.properties with your MySQL password
REM 3. Double-click this file (or run it from the terminal)
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

echo Running...
java -cp "out;lib/*" noirbox.Main

pause
