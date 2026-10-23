@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\HospitalServer.java
if errorlevel 1 (echo Compilation failed. Install JDK 17 or newer. pause & exit /b 1)
java -cp out HospitalServer
pause
