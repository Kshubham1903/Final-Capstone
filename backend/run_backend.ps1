# EduPilot Backend Runner Script
$port = if ($env:SERVER_PORT) { [int]$env:SERVER_PORT } else { 8080 }

# 1. Check for any process currently listening on the target port and attempt termination
$staleConnections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
if ($staleConnections) {
    foreach ($conn in $staleConnections) {
        $pidToKill = $conn.OwningProcess
        if ($pidToKill -and $pidToKill -ne 0) {
            Write-Host "[run_backend.ps1] Found process PID $pidToKill listening on port $port. Attempting cleanup..." -ForegroundColor Yellow
            taskkill /F /PID $pidToKill 2>$null | Out-Null
            if ($LASTEXITCODE -ne 0) {
                Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
            }
        }
    }
    Start-Sleep -Seconds 1
    
    # 2. Check if port is still locked (e.g. requires Administrator rights to kill)
    $stillOccupied = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($stillOccupied) {
        $blockedPid = $stillOccupied[0].OwningProcess
        Write-Host "[run_backend.ps1] WARNING: Port $port is locked by PID $blockedPid (Requires Admin privileges to terminate)." -ForegroundColor Red
        Write-Host "[run_backend.ps1] Automatically switching application port to 8081..." -ForegroundColor Yellow
        $port = 8081
    }
}

Write-Host "[run_backend.ps1] Starting EduPilot Backend on port $port..." -ForegroundColor Green
$env:SERVER_PORT = $port
& "$PSScriptRoot\.maven_portable\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run
