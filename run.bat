@echo off
setlocal
cd /d "%~dp0"

if exist GuessMarketJavaFx.jar (
  set APP_JAR=GuessMarketJavaFx.jar
  set ENGINE_JAR=GuessMarketEngine.jar
  set FX_PATH=lib
) else (
  set APP_JAR=dist\GuessMarketJavaFx.jar
  set ENGINE_JAR=dist\GuessMarketEngine.jar
  if exist dist\lib set FX_PATH=dist\lib
  if not exist dist\lib set FX_PATH=lib
)

java --module-path "%FX_PATH%" --add-modules javafx.controls --enable-native-access=javafx.graphics -cp "%APP_JAR%;%ENGINE_JAR%" guessmarket.ui.GuessMarketApp
endlocal
