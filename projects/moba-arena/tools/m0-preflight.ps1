param(
    [string]$ProjectRoot = (Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path))
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

Add-Type -AssemblyName System.IO.Compression.FileSystem

$lockPath = Join-Path $ProjectRoot 'M0_RUNTIME_LOCK.json'
$modsDir = Join-Path $ProjectRoot 'local\m0\mods'
$reportDir = Join-Path $ProjectRoot 'local\m0\report'

if (-not (Test-Path $lockPath)) {
    throw "Missing runtime lock: $lockPath"
}

New-Item -ItemType Directory -Force -Path $modsDir | Out-Null
New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

$lock = Get-Content -Raw -LiteralPath $lockPath | ConvertFrom-Json
$required = @($lock.runtimeDependencies | Where-Object { $_.requiredFor -eq 'M0' })

function Get-JavaInfo {
    $result = [ordered]@{
        found = $false
        command = $null
        raw = $null
        java17 = $false
        javaHome = $env:JAVA_HOME
    }

    try {
        $javaCommand = Get-Command java -ErrorAction Stop
        $result.found = $true
        $result.command = $javaCommand.Source
        $raw = (& java -version 2>&1 | Out-String).Trim()
        $result.raw = $raw
        if ($raw -match 'version\s+"17(?:\.|\")') {
            $result.java17 = $true
        }
    }
    catch {
        $result.raw = $_.Exception.Message
    }

    [pscustomobject]$result
}

function Get-ZipEntryText {
    param(
        [System.IO.Compression.ZipArchive]$Archive,
        [string]$Name
    )

    $entry = $Archive.GetEntry($Name)
    if ($null -eq $entry) { return $null }

    $stream = $entry.Open()
    try {
        $reader = New-Object System.IO.StreamReader($stream)
        try { return $reader.ReadToEnd() }
        finally { $reader.Dispose() }
    }
    finally { $stream.Dispose() }
}

function Parse-ModsToml {
    param([string]$Text)

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return [pscustomobject]@{ modIds = @(); versions = @(); rawPresent = $false }
    }

    $modIds = @([regex]::Matches($Text, '(?im)^\s*modId\s*=\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique)
    $versions = @([regex]::Matches($Text, '(?im)^\s*version\s*=\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique)

    [pscustomobject]@{
        modIds = $modIds
        versions = $versions
        rawPresent = $true
    }
}

$javaInfo = Get-JavaInfo
$records = @()
$missing = @()
$invalid = @()
$animeSymbols = @()
$animeModsToml = $null
$animeManifest = $null

$symbolPattern = '(?i)(moba|team|shop|store|start|ready|skill|ability|character|select|health|bar|minimap|map|npc|player|capab|network|packet|message|gui|screen|menu|command|procedure|death|respawn|currency|money|gold|equipment|item)'

foreach ($dep in $required) {
    $jarPath = Join-Path $modsDir $dep.expectedFilename
    $record = [ordered]@{
        id = $dep.id
        version = $dep.version
        expectedFilename = $dep.expectedFilename
        source = $dep.source
        exists = (Test-Path -LiteralPath $jarPath)
        sizeBytes = $null
        sha256 = $null
        zipReadable = $false
        modsTomlPresent = $false
        manifestPresent = $false
        modIds = @()
        declaredVersions = @()
        classCount = $null
        error = $null
    }

    if (-not $record.exists) {
        $missing += $dep.expectedFilename
        $records += [pscustomobject]$record
        continue
    }

    try {
        $item = Get-Item -LiteralPath $jarPath
        $record.sizeBytes = $item.Length
        $record.sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $jarPath).Hash.ToLowerInvariant()

        $archive = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
        try {
            $record.zipReadable = $true
            $modsToml = Get-ZipEntryText -Archive $archive -Name 'META-INF/mods.toml'
            $manifest = Get-ZipEntryText -Archive $archive -Name 'META-INF/MANIFEST.MF'
            $parsed = Parse-ModsToml -Text $modsToml
            $record.modsTomlPresent = $parsed.rawPresent
            $record.manifestPresent = -not [string]::IsNullOrWhiteSpace($manifest)
            $record.modIds = @($parsed.modIds)
            $record.declaredVersions = @($parsed.versions)

            $classes = @($archive.Entries | Where-Object { $_.FullName.EndsWith('.class') } | ForEach-Object { $_.FullName })
            $record.classCount = $classes.Count

            if ($dep.id -eq 'anime_assembly') {
                $animeModsToml = $modsToml
                $animeManifest = $manifest
                $animeSymbols = @($classes | Where-Object { $_ -match $symbolPattern } | Sort-Object -Unique)
            }
        }
        finally {
            $archive.Dispose()
        }
    }
    catch {
        $record.error = $_.Exception.Message
        $invalid += $dep.expectedFilename
    }

    $records += [pscustomobject]$record
}

$summary = [ordered]@{
    schemaVersion = 1
    generatedAtUtc = [DateTime]::UtcNow.ToString('o')
    project = 'moba-arena'
    platformExpected = $lock.platform
    projectRoot = $ProjectRoot
    modsDirectory = $modsDir
    java = $javaInfo
    requiredDependencyCount = $required.Count
    presentDependencyCount = @($records | Where-Object { $_.exists }).Count
    readableDependencyCount = @($records | Where-Object { $_.zipReadable }).Count
    missingFiles = $missing
    invalidFiles = $invalid
    dependencies = $records
    animeAssemblySymbolCandidateCount = $animeSymbols.Count
    fingerprintAuditPassed = (($missing.Count -eq 0) -and ($invalid.Count -eq 0) -and (@($records | Where-Object { -not $_.zipReadable }).Count -eq 0))
    java17Detected = [bool]$javaInfo.java17
    runtimeBootTested = $false
    note = 'Fingerprint success does not mean Forge/Anime Assembly runtime compatibility has been tested.'
}

$fingerprintPath = Join-Path $reportDir 'M0_LOCAL_FINGERPRINTS.json'
$summary | ConvertTo-Json -Depth 10 | Set-Content -Encoding UTF8 -LiteralPath $fingerprintPath

$symbolPath = Join-Path $reportDir 'ANIME_ASSEMBLY_SYMBOL_CANDIDATES.txt'
@(
    '# Raw Anime Assembly class-name candidates for M0 symbol audit'
    '# Generated from class entry names only; this is not a verified public API map.'
    "# Candidate count: $($animeSymbols.Count)"
    ''
    $animeSymbols
) | Set-Content -Encoding UTF8 -LiteralPath $symbolPath

$modsTomlPath = Join-Path $reportDir 'ANIME_ASSEMBLY_MODS_TOML.txt'
if ($null -ne $animeModsToml) {
    $animeModsToml | Set-Content -Encoding UTF8 -LiteralPath $modsTomlPath
}
else {
    '# Anime Assembly mods.toml not available because the JAR is missing or unreadable.' | Set-Content -Encoding UTF8 -LiteralPath $modsTomlPath
}

$manifestPath = Join-Path $reportDir 'ANIME_ASSEMBLY_MANIFEST.txt'
if ($null -ne $animeManifest) {
    $animeManifest | Set-Content -Encoding UTF8 -LiteralPath $manifestPath
}
else {
    '# Anime Assembly manifest not available because the JAR is missing or unreadable.' | Set-Content -Encoding UTF8 -LiteralPath $manifestPath
}

Write-Host ''
Write-Host 'MOBA Arena M0 donor intake audit'
Write-Host '--------------------------------'
Write-Host ("Required JARs : {0}" -f $required.Count)
Write-Host ("Present       : {0}" -f $summary.presentDependencyCount)
Write-Host ("Readable JARs : {0}" -f $summary.readableDependencyCount)
Write-Host ("Java 17       : {0}" -f $summary.java17Detected)
Write-Host ("Symbol hits   : {0}" -f $animeSymbols.Count)
Write-Host ("Report        : {0}" -f $fingerprintPath)

if ($missing.Count -gt 0) {
    Write-Host ''
    Write-Host 'Missing exact M0 files:' -ForegroundColor Yellow
    $missing | ForEach-Object { Write-Host (" - {0}" -f $_) -ForegroundColor Yellow }
}

if ($invalid.Count -gt 0) {
    Write-Host ''
    Write-Host 'Unreadable/invalid JARs:' -ForegroundColor Red
    $invalid | ForEach-Object { Write-Host (" - {0}" -f $_) -ForegroundColor Red }
}

if (-not $summary.java17Detected) {
    Write-Host ''
    Write-Host 'Java 17 was not detected. Fingerprinting can still run, but the M0 Forge profile must use Java 17.' -ForegroundColor Yellow
}

if (-not $summary.fingerprintAuditPassed) {
    exit 2
}

Write-Host ''
Write-Host 'Fingerprint audit PASS. Runtime smoke test is still NOT RUN.' -ForegroundColor Green
exit 0
