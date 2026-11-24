$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$DestRoot = Join-Path $Root 'mats\logs'

$Targets = @(
    @{ Name = '1.20.1-fabric';    RunDir = (Join-Path $Root '1.20.1\fabric\run') },
    @{ Name = '1.20.1-forge';     RunDir = (Join-Path $Root '1.20.1\forge\run') },
    @{ Name = '1.21.1-fabric';    RunDir = (Join-Path $Root '1.21.1\fabric\run') },
    @{ Name = '1.21.1-neoforge';  RunDir = (Join-Path $Root '1.21.1\neoforge\run') },
    @{ Name = '1.21.11-fabric';   RunDir = (Join-Path $Root '1.21.11\fabric\run') },
    @{ Name = '1.21.11-neoforge'; RunDir = (Join-Path $Root '1.21.11\neoforge\run') },
    @{ Name = '26.1-fabric';      RunDir = (Join-Path $Root '26.1\fabric\run') },
    @{ Name = '26.1-neoforge';    RunDir = (Join-Path $Root '26.1\neoforge\run') },
    @{ Name = '26.2-fabric';      RunDir = (Join-Path $Root '26.2\fabric\run') },
    @{ Name = '26.2-neoforge';    RunDir = (Join-Path $Root '26.2\neoforge\run') },
    @{ Name = '26.3-fabric';      RunDir = (Join-Path $Root '26.3\fabric\run') },
    @{ Name = '26.3-neoforge';    RunDir = (Join-Path $Root '26.3\neoforge\run') }
)

New-Item -ItemType Directory -Force -Path $DestRoot | Out-Null

foreach ($t in $Targets) {
    $runDir = $t.RunDir
    $targetDir = Join-Path $DestRoot $t.Name

    if (-not (Test-Path $runDir)) {
        Write-Host "[SKIP] $($t.Name): no run/ directory" -ForegroundColor DarkGray
        continue
    }

    if (Test-Path $targetDir) {
        Remove-Item -Path $targetDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

    $copied = 0

    # 1. Minecraft latest.log & debug.log
    foreach ($logName in @('latest.log', 'debug.log')) {
        $logFile = Join-Path $runDir "logs\$logName"
        if (Test-Path $logFile) {
            Copy-Item -Path $logFile -Destination $targetDir -Force
            $copied++
        }
    }

    # 2. Latest crash report (if any)
    $crashDir = Join-Path $runDir 'crash-reports'
    if (Test-Path $crashDir) {
        $latestCrash = Get-ChildItem -Path $crashDir -Filter '*.txt' -File |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($latestCrash) {
            Copy-Item -Path $latestCrash.FullName -Destination $targetDir -Force
            $copied++
        }
    }

    # 3. SimpleMap debug (latest.txt + newest session metrics/events)
    $debugDir = Join-Path $runDir 'simplemap-debug'
    if (Test-Path $debugDir) {
        $destDebug = Join-Path $targetDir 'simplemap-debug'
        New-Item -ItemType Directory -Force -Path $destDebug | Out-Null

        $latestTxt = Join-Path $debugDir 'latest.txt'
        if (Test-Path $latestTxt) {
            Copy-Item -Path $latestTxt -Destination $destDebug -Force
            $copied++
        }

        $latestSession = Get-ChildItem -Path $debugDir -Directory |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($latestSession) {
            $destSession = Join-Path $destDebug $latestSession.Name
            Copy-Item -Path $latestSession.FullName -Destination $destSession -Recurse -Force
            $copied++
        }
    }

    Write-Host "[OK] $($t.Name) -> mats\logs\$($t.Name)\ ($copied item(s))" -ForegroundColor Green
}
