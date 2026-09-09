@echo off
setlocal
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
    echo ERROR: javac was not found. Install JDK 25 and add it to PATH.
    exit /b 1
)

if not exist lib\javafx.controls.jar (
    echo ERROR: JavaFX libraries are missing in lib\
    echo Run download-javafx.bat first, or copy JavaFX 25 win jars into lib\
    exit /b 1
)

if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build\engine
mkdir build\ui
mkdir dist
mkdir dist\lib

for /r GuessMarketEngine\src %%f in (*.java) do echo %%f>>build\engine-sources.txt
javac -encoding UTF-8 -d build\engine @build\engine-sources.txt
if errorlevel 1 exit /b 1
jar --create --file dist\GuessMarketEngine.jar -C build\engine .

mkdir build\ui\guessmarket\ui
copy /Y GuessMarketJavaFx\src\guessmarket\ui\app.css build\ui\guessmarket\ui\app.css >nul

for /r GuessMarketJavaFx\src %%f in (*.java) do echo %%f>>build\ui-sources.txt
javac -encoding UTF-8 --module-path lib --add-modules javafx.controls -cp dist\GuessMarketEngine.jar -d build\ui @build\ui-sources.txt
if errorlevel 1 exit /b 1

(
  echo Manifest-Version: 1.0
  echo Main-Class: guessmarket.ui.GuessMarketApp
  echo Class-Path: GuessMarketEngine.jar
  echo.
)>build\ui-manifest.mf

jar --create --file dist\GuessMarketJavaFx.jar --manifest build\ui-manifest.mf -C build\ui .
if errorlevel 1 exit /b 1

copy /Y lib\*.jar dist\lib\ >nul
copy /Y run.bat dist\run.bat >nul

echo Build completed.
echo Output: dist\GuessMarketEngine.jar dist\GuessMarketJavaFx.jar
endlocal
