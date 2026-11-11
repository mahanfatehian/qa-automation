Set-Location -Path $PSScriptRoot
docker compose up --build -d --scale chrome-node=2
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host ""
Write-Host "BlueprintQA is starting."
Write-Host "  Studio    http://localhost:3000"
Write-Host "  API docs  http://localhost:8000/docs"
Write-Host "  Grid      http://localhost:4444"
