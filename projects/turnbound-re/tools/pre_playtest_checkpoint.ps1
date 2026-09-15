param(
    [switch]$SkipDependencies
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradlew = Join-Path $projectRoot 'gradlew.bat'
$gradleProperties = Join-Path $projectRoot 'gradle.properties'

if (-not (Test-Path $gradlew)) {
    throw "gradlew.bat not found at $gradlew"
}
if (-not (Test-Path $gradleProperties)) {
    throw "gradle.properties not found at $gradleProperties"
}

Push-Location $projectRoot
try {
    Write-Host 'TURNBOUND: RE local pre-playtest checkpoint'
    Write-Host "Project: $projectRoot"

    $javaVersion = (& java -version 2>&1 | Out-String)
    Write-Host $javaVersion.Trim()
    if ($javaVersion -notmatch 'version "25(?:\.|\")') {
        throw 'Java 25 is required for this project.'
    }

    & $gradlew --version
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle wrapper check failed with exit code $LASTEXITCODE"
    }

    $args = @('--no-daemon')
    if (-not $SkipDependencies) {
        $args += 'dependencies'
    }
    $args += @('clean', 'build', '--stacktrace')

    Write-Host "Running: gradlew.bat $($args -join ' ')"
    & $gradlew @args
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle checkpoint failed with exit code $LASTEXITCODE"
    }

    $jars = @(Get-ChildItem -Path (Join-Path $projectRoot 'build\libs') -Filter 'turnbound_re-*.jar' -File |
        Where-Object { $_.Name -notmatch '-sources\.jar$' -and $_.Name -notmatch '-javadoc\.jar$' } |
        Sort-Object Length -Descending)
    if ($jars.Count -lt 1) {
        throw 'No production TURNBOUND JAR found in build/libs.'
    }
    $jar = $jars[0]

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
    try {
        $entries = @($zip.Entries | ForEach-Object { $_.FullName })

        function Assert-Entry([string]$Exact) {
            if ($entries -notcontains $Exact) {
                throw "Required JAR entry missing: $Exact"
            }
        }

        function Assert-Prefix([string]$Prefix) {
            if (-not ($entries | Where-Object { $_.StartsWith($Prefix, [System.StringComparison]::Ordinal) } | Select-Object -First 1)) {
                throw "Required JAR path missing: $Prefix"
            }
        }

        Assert-Entry 'META-INF/neoforge.mods.toml'
        Assert-Entry 'kr/moonseungjun/turnboundre/TurnboundRe.class'
        Assert-Prefix 'assets/turnbound_re/'
        Assert-Prefix 'data/turnbound_re/'

        $sourceEntries = @($entries | Where-Object { $_ -match '\.(java|kt|groovy)$' })
        if ($sourceEntries.Count -gt 0) {
            throw "Source file found in production JAR: $($sourceEntries[0])"
        }

        $devEntries = @($entries | Where-Object { $_ -match '(^|/)(\.github|tools)/' })
        if ($devEntries.Count -gt 0) {
            throw "Development-only path found in production JAR: $($devEntries[0])"
        }

        $duplicates = @($entries | Group-Object | Where-Object Count -gt 1)
        if ($duplicates.Count -gt 0) {
            throw "Duplicate JAR entry: $($duplicates[0].Name)"
        }
    }
    finally {
        $zip.Dispose()
    }

    $hash = (Get-FileHash -Algorithm SHA256 -Path $jar.FullName).Hash.ToLowerInvariant()
    $props = @{}
    foreach ($line in Get-Content $gradleProperties) {
        if ($line -match '^([^#=]+)=(.*)$') {
            $props[$matches[1].Trim()] = $matches[2].Trim()
        }
    }

    $outDir = Join-Path $projectRoot 'build\pre-playtest-checkpoint'
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $hashLine = "$hash *$($jar.Name)"
    Set-Content -Path (Join-Path $outDir 'turnbound-re.sha256') -Value $hashLine -Encoding ascii

    $commit = ''
    if (Get-Command git -ErrorAction SilentlyContinue) {
        $commit = (& git rev-parse HEAD 2>$null | Out-String).Trim()
    }
    if ([string]::IsNullOrWhiteSpace($commit)) {
        $commit = 'UNKNOWN'
    }

    $report = @"
# TURNBOUND: RE Local Pre-Playtest Checkpoint

- Commit: $commit
- Version: $($props['mod_version'])
- Minecraft: $($props['minecraft_version'])
- Java: 25
- NeoForge: $($props['neo_version'])
- Command: gradlew.bat $($args -join ' ')
- Gradle build/JUnit: PASS
- Production JAR verify: PASS
- JAR: $($jar.FullName)
- SHA-256: $hash
- Datagen: NOT RUN
- GameTest: NOT RUN
- Dedicated server smoke: NOT RUN
- Client smoke: NOT RUN
- Minecraft playtest: NOT RUN
- Drehmal 26.2 migration: NOT RUN by this script

Validation scope: Gradle dependency resolution (unless explicitly skipped), clean build, JUnit, production JAR metadata/class/assets/data presence, source/development-path exclusion, duplicate entry check, SHA-256.
"@
    Set-Content -Path (Join-Path $outDir 'BUILD_AND_RUNTIME_REPORT.md') -Value $report -Encoding utf8

    Write-Host ''
    Write-Host 'LOCAL CHECKPOINT PASS'
    Write-Host "JAR: $($jar.FullName)"
    Write-Host "SHA-256: $hash"
    Write-Host "Report: $(Join-Path $outDir 'BUILD_AND_RUNTIME_REPORT.md')"
    Write-Host 'NEXT: run the Drehmal 26.2 migration audit. Do not treat this build PASS as PLAYTESTED.'
}
finally {
    Pop-Location
}
