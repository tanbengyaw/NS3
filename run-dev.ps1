# Run NS3 ASSIST backend with Java 17+ only in this process.
# Does NOT change global JAVA_HOME - assist_testing_only / legacy ASSIST keep Java 11.

$ErrorActionPreference = "Stop"

function Get-JavaVersionText {
    param([string]$JavaExe)
    $saved = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    try {
        $lines = & $JavaExe -version 2>&1 | ForEach-Object { "$_" }
        return ($lines -join [Environment]::NewLine)
    } finally {
        $ErrorActionPreference = $saved
    }
}

function Test-Java17OrNewer {
    param([string]$JavaExe)
    $text = Get-JavaVersionText -JavaExe $JavaExe
    return ($text -match 'version "(1[7-9]|[2-9][0-9])')
}

function Find-ModernJdkHome {
    $searchPaths = @(
        $env:NS3_JAVA_HOME,
        (Get-ChildItem "C:\Program Files\Java\jdk-17*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName),
        (Get-ChildItem "C:\Program Files\Java\jdk-21*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName),
        (Get-ChildItem "C:\Program Files\Microsoft\jdk-17*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName),
        (Get-ChildItem "C:\Program Files\Microsoft\jdk-21*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName),
        (Get-ChildItem "C:\Program Files\Eclipse Adoptium\jdk-17*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName),
        (Get-ChildItem "C:\Program Files\Eclipse Adoptium\jdk-21*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName)
    ) | Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\java.exe")) }

    foreach ($jdkHome in $searchPaths) {
        $javaExe = Join-Path $jdkHome "bin\java.exe"
        if ($jdkHome -match 'jdk-1[7-9]|jdk-2[0-9]|jdk-17|jdk-21') {
            return $jdkHome
        }
        if (Test-Java17OrNewer -JavaExe $javaExe) {
            return $jdkHome
        }
    }
    return $null
}

$jdk = Find-ModernJdkHome
if (-not $jdk) {
    Write-Host ""
    Write-Host "Java 17+ not found. Install JDK 21 (global JAVA_HOME can stay on Java 11):" -ForegroundColor Yellow
    Write-Host "  winget install Microsoft.OpenJDK.21 --accept-package-agreements --accept-source-agreements"
    Write-Host ""
    Write-Host "Or set NS3_JAVA_HOME, e.g.:"
    Write-Host '  [System.Environment]::SetEnvironmentVariable("NS3_JAVA_HOME", "C:\Program Files\Java\jdk-17", "User")'
    Write-Host ""
    exit 1
}

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repoRoot

# Ensure cmd.exe is available for gradlew.bat (some shells have a minimal PATH)
$system32 = Join-Path $env:SystemRoot "System32"
if ((Test-Path $system32) -and ($env:Path -notlike "*$system32*")) {
    $env:Path = "$system32;$env:Path"
}

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;" + ($env:Path -split ';' | Where-Object {
    $_ -and
    $_ -notmatch '\\Java\\jdk-11' -and
    $_ -notmatch '\\Java\\jdk1\.7' -and
    $_ -notmatch '\\Java\\jdk1\.8' -and
    $_ -notmatch '\\Common Files\\Oracle\\Java\\javapath'
}) -join ';'
$env:SPRING_PROFILES_ACTIVE = "dev"

Write-Host "NS3 ASSIST - dev profile (file-backed H2)" -ForegroundColor Cyan
Write-Host "JAVA_HOME = $env:JAVA_HOME"
Write-Host (Get-JavaVersionText -JavaExe (Join-Path $jdk "bin\java.exe"))
Write-Host ""
Write-Host "DB file:  ./data/ns3_assist.mv.db (DBeaver - see README)" -ForegroundColor DarkGray
Write-Host "API:      http://localhost:8081/assist-provider/api/v1/employers"
Write-Host "Swagger:  http://localhost:8081/assist-provider/swagger-ui/index.html"
Write-Host "Login:    admin / password"
Write-Host ""

$gradlew = Join-Path $repoRoot "gradlew.bat"
& $gradlew :assist-provider:bootRun
