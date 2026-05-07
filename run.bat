@echo off
cd /d "%~dp0"
call mvn -DskipTests package
if errorlevel 1 (
    pause
    exit /b 1
)
java -jar target\online-exam-system-1.0.0.jar
pause
