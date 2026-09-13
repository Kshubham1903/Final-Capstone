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

# Validate and print masked status for LLM keys
Write-Host "========== EDUPILOT ENVIRONMENT VALIDATION ==========" -ForegroundColor Cyan
Write-Host "LLM Provider selected: $env:LLM_PROVIDER" -ForegroundColor Cyan
Write-Host "Groq Model selected: $env:GROQ_MODEL" -ForegroundColor Cyan

if (-not $env:GROQ_API_KEY -or $env:GROQ_API_KEY.Length -lt 10) {
    Write-Host "[ERROR] GROQ_API_KEY is missing or invalid in process environment and .env files!" -ForegroundColor Red
    Write-Host "Please set a valid GROQ_API_KEY in .env or system environment variables before starting." -ForegroundColor Red
    exit 1
} else {
    $maskedGroq = $env:GROQ_API_KEY.Substring(0, [Math]::Min(7, $env:GROQ_API_KEY.Length)) + "..." + $env:GROQ_API_KEY.Substring([Math]::Max(0, $env:GROQ_API_KEY.Length - 4))
    Write-Host "GROQ_API_KEY loaded successfully: $maskedGroq (Length: $($env:GROQ_API_KEY.Length))" -ForegroundColor Green
}

if (-not $env:GEMINI_API_KEY -or $env:GEMINI_API_KEY.Length -lt 10) {
    Write-Host "[WARNING] GEMINI_API_KEY is missing or invalid in .env files." -ForegroundColor Yellow
} else {
    $maskedGemini = $env:GEMINI_API_KEY.Substring(0, [Math]::Min(4, $env:GEMINI_API_KEY.Length)) + "..." + $env:GEMINI_API_KEY.Substring([Math]::Max(0, $env:GEMINI_API_KEY.Length - 4))
    Write-Host "GEMINI_API_KEY loaded successfully: $maskedGemini (Length: $($env:GEMINI_API_KEY.Length))" -ForegroundColor Green
}
Write-Host "=====================================================" -ForegroundColor Cyan

$port = if ($env:SERVER_PORT) { [int]$env:SERVER_PORT } else { 8085 }

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
}

Write-Host "[run_backend.ps1] Starting EduPilot Backend on port $port..." -ForegroundColor Green
$env:SERVER_PORT = $port
& "$PSScriptRoot\.maven_portable\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run
