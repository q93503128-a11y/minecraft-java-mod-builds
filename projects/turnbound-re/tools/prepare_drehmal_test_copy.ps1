param(
    [string]$Root = (Join-Path (Get-Location) 'drehmal-test'),
    [switch]$SkipResourcePack
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$downloads = Join-Path $Root 'downloads'
$worldOut = Join-Path $Root 'world'

$shards = @(
    @{
        Name = 'shard_1.zip'
        Url = 'https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/shard_1.zip'
        Sha256 = '378f8bea9c88371c44d3a6fd6c6d91a886f14b6dfce97e6e6e994789b979b358'
    },
    @{
        Name = 'shard_2.zip'
        Url = 'https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/shard_2.zip'
        Sha256 = '2d250f04259404d81a3e9bd83a5592f1c26545f45f9c8a22a2277c9e523c1331'
    },
    @{
        Name = 'shard_3.zip'
        Url = 'https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/shard_3.zip'
        Sha256 = '8c892c77c7ab9ef03aac7b517e5448490f55690163085b38c380523dd2d29b67'
    }
)

$resource = @{
    Name = 'resources.zip'
    Url = 'https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/resources.zip'
    Size = 190255507L
}

function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $Name"
    }
}

function Download-File([string]$Url, [string]$Target) {
    if (Test-Path $Target) {
        Write-Host "Using existing download: $Target"
        return
    }

    Write-Host "Downloading $Url"
    & curl.exe -L --fail --retry 5 --retry-delay 3 -o $Target $Url
    if ($LASTEXITCODE -ne 0) {
        throw "Download failed with curl exit code $LASTEXITCODE: $Url"
    }
}

function Assert-Sha256([string]$Path, [string]$Expected) {
    $actual = (Get-FileHash -Algorithm SHA256 -Path $Path).Hash.ToLowerInvariant()
    if ($actual -ne $Expected.ToLowerInvariant()) {
        throw "SHA-256 mismatch for $Path`nexpected=$Expected`nactual=$actual"
    }
    Write-Host "SHA-256 OK: $(Split-Path $Path -Leaf)"
}

Require-Command 'curl.exe'
Require-Command 'tar.exe'

New-Item -ItemType Directory -Force -Path $downloads | Out-Null
New-Item -ItemType Directory -Force -Path $Root | Out-Null

if (Test-Path $worldOut) {
    $existing = @(Get-ChildItem -Force -Path $worldOut -ErrorAction SilentlyContinue)
    if ($existing.Count -gt 0) {
        throw "Refusing to merge into a non-empty world directory: $worldOut`nMove/delete that TEST COPY directory and run again. Never point this script at the original Drehmal save."
    }
} else {
    New-Item -ItemType Directory -Path $worldOut | Out-Null
}

Write-Host 'Drehmal: APOTHEOSIS v2.2.2f migration-copy preparation'
Write-Host 'Expected official payload: ~3.99 GB compressed / ~5.14 GB map uncompressed.'
Write-Host 'Keep substantially more free disk space for downloads, extraction, Minecraft migration backup, and logs.'

foreach ($shard in $shards) {
    $target = Join-Path $downloads $shard.Name
    Download-File $shard.Url $target
    Assert-Sha256 $target $shard.Sha256
}

if (-not $SkipResourcePack) {
    $resourceTarget = Join-Path $downloads $resource.Name
    Download-File $resource.Url $resourceTarget
    $actualSize = (Get-Item $resourceTarget).Length
    if ($actualSize -ne $resource.Size) {
        throw "Unexpected resources.zip size. expected=$($resource.Size) actual=$actualSize"
    }
    Write-Host 'resources.zip size matches the official GitHub release metadata. No official release digest is published for this asset.'
}

foreach ($shard in $shards) {
    $path = Join-Path $downloads $shard.Name
    Write-Host "Extracting $($shard.Name) into the shared test output..."
    & tar.exe -xf $path -C $worldOut
    if ($LASTEXITCODE -ne 0) {
        throw "Extraction failed with tar exit code $LASTEXITCODE: $path"
    }
}

$levelFiles = @(Get-ChildItem -Path $worldOut -Filter 'level.dat' -File -Recurse)
if ($levelFiles.Count -ne 1) {
    throw "Expected exactly one level.dat after shard assembly; found $($levelFiles.Count). Inspect $worldOut before attempting Minecraft migration."
}

$saveRoot = $levelFiles[0].Directory.FullName
Write-Host ''
Write-Host 'Official shard download/hash checks: PASS'
Write-Host 'Shard assembly: PASS'
Write-Host "Detected Minecraft save root: $saveRoot"
Write-Host "Resource pack: $(if ($SkipResourcePack) { 'SKIPPED' } else { Join-Path $downloads $resource.Name })"
Write-Host ''
Write-Host 'NEXT: copy the detected save root into a Minecraft 26.2 TEST saves directory. Do not overwrite the original 1.20.1 source.'
Write-Host 'Then follow docs/34_DREHMAL_26_2_MIGRATION_AUDIT.md. This script does NOT claim Minecraft 26.2 migration success.'
