Set-Location -Path $PSScriptRoot
$count = 2
$countFile = Join-Path $PSScriptRoot "runtime\browsers.txt"
if (Test-Path $countFile) {
  $raw = Get-Content $countFile -TotalCount 1
  if ($raw -match '^\d+$') { $count = [int]$raw }
}
docker compose up --build -d --scale "chrome-node=$count"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host ""
Write-Host "BlueprintQA is starting."
Write-Host "  Studio    http://localhost:3000"
Write-Host "  API docs  http://localhost:8000/docs"
Write-Host "  Grid      http://localhost:4444"
