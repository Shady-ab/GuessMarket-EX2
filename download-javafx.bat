@echo off
setlocal
cd /d "%~dp0"
mkdir lib 2>nul

set BASE=https://repo1.maven.org/maven2/org/openjfx
set VER=25.0.1

echo Downloading JavaFX %VER% Windows jars...
curl -L "%BASE%/javafx-base/%VER%/javafx-base-%VER%-win.jar" -o lib\javafx.base.jar
if errorlevel 1 exit /b 1
curl -L "%BASE%/javafx-graphics/%VER%/javafx-graphics-%VER%-win.jar" -o lib\javafx.graphics.jar
if errorlevel 1 exit /b 1
curl -L "%BASE%/javafx-controls/%VER%/javafx-controls-%VER%-win.jar" -o lib\javafx.controls.jar
if errorlevel 1 exit /b 1

echo JavaFX jars are in lib\
endlocal
