@echo off
setlocal
cd /d "%~dp0"
docker compose up --build -d --scale chrome-node=2
if errorlevel 1 exit /b 1
echo.
echo BlueprintQA is starting.
echo   Studio    http://localhost:3000
echo   API docs  http://localhost:8000/docs
echo   Grid      http://localhost:4444
echo.
echo First boot builds Java and the UI. Tail logs with: docker compose logs -f
