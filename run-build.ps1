# Build NS3 ASSIST using Java 17+ only in this process (does not change global JAVA_HOME).

$ErrorActionPreference = "Stop"

function Find-ModernJdkHome {
    $searchPaths = @(
        $env:NS3_JAVA_HOME,
        (Get-ChildItem "C:\Program Files\Java\jdk-17*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName),
        (Get-ChildItem "C:\Program Files\Java\jdk-21*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName),
        (Get-ChildItem "C:\Program Files\Microsoft\jdk-17*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName),
        (Get-ChildItem "C:\Program Files\Microsoft\jdk-21*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName)
    ) | Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\java.exe")) }

    foreach ($jdkHome in $searchPaths) {
        if ($jdkHome -match 'jdk-1[7-9]|jdk-2[0-9]|jdk-17|jdk-21') {
            return $jdkHome
        }
    }
    return $null
}

$jdk = Find-ModernJdkHome
if (-not $jdk) {
    Write-Host "Java 17+ not found. Set NS3_JAVA_HOME or install JDK 21." -ForegroundColor Yellow
    exit 1
}

Set-Location $PSScriptRoot

$system32 = Join-Path $env:SystemRoot "System32"
if ((Test-Path $system32) -and ($env:Path -notlike "*$system32*")) {
    $env:Path = "$system32;$env:Path"
}

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"

Write-Host "Building and testing with JAVA_HOME=$jdk" -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "gradlew.bat") build
