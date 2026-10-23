@echo off
setlocal
cd /d "%~dp0"
rem Use the per-user temp directory to avoid stale or unwritable Gradle homes.
set "GRADLE_USER_HOME=%TEMP%\sanya-gradle-cache"
echo Building SANЯ CLIENT for Minecraft 1.21.11...
echo Gradle cache: "%GRADLE_USER_HOME%"
call gradlew.bat build
if errorlevel 1 (
  echo.
  echo Build failed. Keep this window open and send me a screenshot if possible.
) else (
  echo.
  echo Build complete. The mod JAR is in build\libs\
)
echo.
pause
