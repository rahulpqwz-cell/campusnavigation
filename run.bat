@echo off
rem Compile and start the CampusNav Swing application.
set "JAVA_BIN="
if defined JAVA_HOME set "JAVA_BIN=%JAVA_HOME%\bin"
if not defined JAVA_BIN if exist "%ProgramFiles%\Java\jdk-17\bin\javac.exe" set "JAVA_BIN=%ProgramFiles%\Java\jdk-17\bin"
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
if defined JAVA_BIN (
  "%JAVA_BIN%\javac.exe" -encoding UTF-8 -d out @sources.txt
) else (
  javac -encoding UTF-8 -d out @sources.txt
)
if errorlevel 1 (
  echo Compilation failed.
  pause
  exit /b 1
)
del sources.txt
if defined JAVA_BIN (
  "%JAVA_BIN%\java.exe" -cp out campusnav.Main
) else (
  java -cp out campusnav.Main
)
