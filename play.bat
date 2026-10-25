@echo off
rem Starts the already compiled game. Does not run Maven.
cd /d "%~dp0"
set "JAVA=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\javaw.exe"
if not exist "%JAVA%" (
  echo Temurin 17 was not found at:
  echo %JAVA%
  pause
  exit /b 1
)
set "M2=%USERPROFILE%\.m2\repository"
set "FX=%M2%\org\openjfx"
set "MODS=%FX%\javafx-base\18.0.2\javafx-base-18.0.2-win.jar;%FX%\javafx-controls\18.0.2\javafx-controls-18.0.2-win.jar;%FX%\javafx-fxml\18.0.2\javafx-fxml-18.0.2-win.jar;%FX%\javafx-graphics\18.0.2\javafx-graphics-18.0.2-win.jar;%FX%\javafx-media\18.0.2\javafx-media-18.0.2-win.jar;%FX%\javafx-web\18.0.2\javafx-web-18.0.2-win.jar"
set "CP=target\classes;%M2%\org\openjdk\nashorn\nashorn-core\15.4\nashorn-core-15.4.jar;%M2%\org\ow2\asm\asm\7.3.1\asm-7.3.1.jar;%M2%\org\ow2\asm\asm-commons\7.3.1\asm-commons-7.3.1.jar;%M2%\org\ow2\asm\asm-tree\7.3.1\asm-tree-7.3.1.jar;%M2%\org\ow2\asm\asm-util\7.3.1\asm-util-7.3.1.jar"
if not exist "target\classes\com\lilithsthrone\main\Main.class" (
  echo target\classes is missing. Compile once with mvn compile, then double-click this again.
  pause
  exit /b 1
)
start "" "%JAVA%" --module-path "%MODS%" --add-modules javafx.base,javafx.graphics,javafx.controls,javafx.fxml,javafx.media,javafx.web -cp "%CP%" com.lilithsthrone.Launcher
