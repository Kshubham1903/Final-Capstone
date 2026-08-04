$mavenVersion = "3.9.6"
$mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
$zipPath = "$PSScriptRoot/maven.zip"
$destPath = "$PSScriptRoot/.maven_portable"

if (-not (Test-Path $destPath)) {
    Write-Host "Downloading portable Apache Maven $mavenVersion..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $mavenUrl -OutFile $zipPath
    Write-Host "Extracting Maven..." -ForegroundColor Cyan
    Expand-Archive -Path $zipPath -DestinationPath $destPath
    Remove-Item $zipPath
    Write-Host "Maven installed portably at $destPath!" -ForegroundColor Green
} else {
    Write-Host "Portable Maven already installed." -ForegroundColor Yellow
}

# Create corrected run_backend.ps1 using Call Operator & and backslashes
$runScriptContent = "& `"`$PSScriptRoot\.maven_portable\apache-maven-$mavenVersion\bin\mvn.cmd`" spring-boot:run"
$runScriptContent | Out-File -FilePath "$PSScriptRoot/run_backend.ps1" -Encoding utf8
Write-Host "Created run_backend.ps1. Run '.\run_backend.ps1' to boot the backend!" -ForegroundColor Green
