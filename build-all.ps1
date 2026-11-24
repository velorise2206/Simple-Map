$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
$OutDir = Join-Path (Split-Path -Parent $Root) 'mods\simplemap'
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Resolve-Jdk([string]$EnvVar, [string[]]$Patterns) {
    $val = [Environment]::GetEnvironmentVariable($EnvVar)
    if ($val -and (Test-Path (Join-Path $val 'bin\java.exe'))) { return $val }
    foreach ($pat in $Patterns) {
        $match = Get-ChildItem -Path $pat -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
        if ($match -and (Test-Path (Join-Path $match.FullName 'bin\java.exe'))) { return $match.FullName }
    }
    return $null
}

$buildResults = [System.Collections.Generic.List[PSCustomObject]]::new()

function Invoke-Build([string]$Label, [string]$RelDir, [string]$JavaVar, [string]$JdkName) {
    $verNum = switch ($JavaVar) {
        'JAVA17_HOME' { '17' }
        'JAVA21_HOME' { '21' }
        'JAVA25_HOME' { '25' }
    }
    $patterns = @(
        "C:\Program Files\Eclipse Adoptium\jdk-$verNum*",
        "C:\Program Files\Java\jdk-$verNum*",
        "C:\Program Files\Microsoft\jdk-$verNum*",
        "C:\Program Files\Zulu\zulu-$verNum*",
        "C:\Program Files\Amazon Corretto\jdk$verNum*",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\jdk-$verNum*",
        "$env:USERPROFILE\.jdks\*$verNum*"
    )
    $jdk = Resolve-Jdk $JavaVar $patterns
    if (-not $jdk) { throw "Missing $JdkName. Set $JavaVar or install $JdkName." }
    Write-Host "`n=== Building $Label ($RelDir) with $jdk ===" -ForegroundColor Cyan
    Push-Location (Join-Path $Root $RelDir)
    try {
        $env:JAVA_HOME = $jdk
        $env:PATH = "$jdk\bin;$env:PATH"
        & .\gradlew.bat build --stacktrace
        if ($LASTEXITCODE -ne 0) { throw "Build failed for $Label" }
        $jar = Get-ChildItem -Path 'build\libs\*.jar' | Where-Object {
            $_.Name -notmatch 'sources|dev|javadoc' -and $_.Name -match '2\.1\.0'
        } | Sort-Object LastWriteTime -Descending | Select-Object -First 1

        if (-not $jar) {
            $jar = Get-ChildItem -Path 'build\libs\*.jar' | Where-Object {
                $_.Name -notmatch 'sources|dev|javadoc'
            } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        }

        if ($jar) {
            Copy-Item $jar.FullName -Destination $OutDir -Force
            Write-Host "[OK] Copied $($jar.Name) -> $OutDir" -ForegroundColor Green
            $script:buildResults.Add([PSCustomObject]@{
                Target = $Label
                Jar    = $jar.Name
                Size   = "$([math]::Round($jar.Length / 1MB, 2)) MB"
                Status = 'OK'
            })
        } else {
            throw "No output jar found in $RelDir\build\libs"
        }
    } finally {
        Pop-Location
    }
}

Invoke-Build 'Fabric 1.20.1'    '1.20.1\fabric'    'JAVA17_HOME' 'JDK 17'
Invoke-Build 'Forge 1.20.1'     '1.20.1\forge'     'JAVA17_HOME' 'JDK 17'
Invoke-Build 'Fabric 1.21.1'    '1.21.1\fabric'    'JAVA21_HOME' 'JDK 21'
Invoke-Build 'NeoForge 1.21.1'  '1.21.1\neoforge'  'JAVA21_HOME' 'JDK 21'
Invoke-Build 'Fabric 1.21.11'   '1.21.11\fabric'   'JAVA21_HOME' 'JDK 21'
Invoke-Build 'NeoForge 1.21.11' '1.21.11\neoforge' 'JAVA21_HOME' 'JDK 21'
Invoke-Build 'Fabric 26.1'      '26.1\fabric'      'JAVA25_HOME' 'JDK 25'
Invoke-Build 'NeoForge 26.1'    '26.1\neoforge'    'JAVA25_HOME' 'JDK 25'
Invoke-Build 'Fabric 26.2'      '26.2\fabric'      'JAVA25_HOME' 'JDK 25'
Invoke-Build 'NeoForge 26.2'    '26.2\neoforge'    'JAVA25_HOME' 'JDK 25'
Invoke-Build 'Fabric 26.3'      '26.3\fabric'      'JAVA25_HOME' 'JDK 25'
Invoke-Build 'NeoForge 26.3'    '26.3\neoforge'    'JAVA25_HOME' 'JDK 25'

Write-Host "`nAll 12 Simple Map builds completed! Output: $OutDir" -ForegroundColor Green
$buildResults | Format-Table -AutoSize
