@echo off
setlocal
cd /d "%~dp0"
set COUNT=2
if exist runtime\browsers.txt set /p COUNT=<runtime\browsers.txt
docker compose up --build -d --scale chrome-node=%COUNT%
if errorlevel 1 exit /b 1
echo.
echo BlueprintQA is starting.
echo   Studio    http://localhost:3000
echo   API docs  http://localhost:8000/docs
echo   Grid      http://localhost:4444
echo.
echo First boot builds Java and the UI. Tail logs with: docker compose logs -f
