# Reset local dev H2 database. Run this when startup fails with Liquibase / databasechangelog errors.
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repoRoot

Write-Host "Stopping processes on port 8081 (if any)..." -ForegroundColor Yellow
Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique |
    ForEach-Object {
        if ($_ -and $_ -ne 0) {
            Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
        }
    }

Start-Sleep -Seconds 1

$patterns = @(
    "data\ns3_assist.mv.db",
    "data\ns3_assist.trace.db",
    "data\ns3_assist.lock.db",
    "assist-provider\data\ns3_assist.mv.db",
    "assist-provider\data\ns3_assist.trace.db",
    "assist-provider\data\ns3_assist.lock.db"
)

$removed = 0
foreach ($relative in $patterns) {
    $path = Join-Path $repoRoot $relative
    if (Test-Path $path) {
        Remove-Item $path -Force
        Write-Host "Removed $relative" -ForegroundColor DarkGray
        $removed++
    }
}

if ($removed -eq 0) {
    Write-Host "No dev database files found to remove." -ForegroundColor DarkGray
} else {
    Write-Host "Dev database reset ($removed file(s) removed)." -ForegroundColor Green
}

Write-Host ""
Write-Host "Next: .\run-dev.ps1" -ForegroundColor Cyan
