# EduPilot Backend Runner Script

# Ensure GROQ_API_KEY and LLM_PROVIDER process environment variables are resolved
if (-not $env:GROQ_API_KEY) {
    $userKey = [System.Environment]::GetEnvironmentVariable('GROQ_API_KEY', 'User')
    $machineKey = [System.Environment]::GetEnvironmentVariable('GROQ_API_KEY', 'Machine')
    if ($userKey) { $env:GROQ_API_KEY = $userKey }
    elseif ($machineKey) { $env:GROQ_API_KEY = $machineKey }
    elseif (Test-Path "$PSScriptRoot\.env") {
        Get-Content "$PSScriptRoot\.env" | ForEach-Object {
            if ($_ -match '^\s*GROQ_API_KEY\s*=\s*(.*)\s*$') { $env:GROQ_API_KEY = $matches[1].Trim('"''') }
        }
    } elseif (Test-Path "$PSScriptRoot\..\.env") {
        Get-Content "$PSScriptRoot\..\.env" | ForEach-Object {
            if ($_ -match '^\s*GROQ_API_KEY\s*=\s*(.*)\s*$') { $env:GROQ_API_KEY = $matches[1].Trim('"''') }
        }
    }
}

if (-not $env:LLM_PROVIDER) {
    $userProv = [System.Environment]::GetEnvironmentVariable('LLM_PROVIDER', 'User')
    $machineProv = [System.Environment]::GetEnvironmentVariable('LLM_PROVIDER', 'Machine')
    if ($userProv) { $env:LLM_PROVIDER = $userProv }
    elseif ($machineProv) { $env:LLM_PROVIDER = $machineProv }
    elseif (Test-Path "$PSScriptRoot\.env") {
        Get-Content "$PSScriptRoot\.env" | ForEach-Object {
            if ($_ -match '^\s*LLM_PROVIDER\s*=\s*(.*)\s*$') { $env:LLM_PROVIDER = $matches[1].Trim('"''') }
        }
    } elseif (Test-Path "$PSScriptRoot\..\.env") {
        Get-Content "$PSScriptRoot\..\.env" | ForEach-Object {
            if ($_ -match '^\s*LLM_PROVIDER\s*=\s*(.*)\s*$') { $env:LLM_PROVIDER = $matches[1].Trim('"''') }
        }
    } else { $env:LLM_PROVIDER = 'groq' }
}

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
