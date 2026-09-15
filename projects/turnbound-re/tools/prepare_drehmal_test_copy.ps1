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

function Get-Sha256([string]$Path) {
    return (Get-FileHash -Algorithm SHA256 -Path $Path).Hash.ToLowerInvariant()
}

function Download-Fresh([string]$Url, [string]$Target) {
    $part = "$Target.part"
    if (Test-Path $part) {
        Remove-Item -Force $part
    }

    Write-Host "Downloading $Url"
    & curl.exe -L --fail --retry 5 --retry-delay 3 -o $part $Url
    if ($LASTEXITCODE -ne 0) {
        throw "Download failed with curl exit code $LASTEXITCODE: $Url"
    }
    Move-Item -Force $part $Target
}

function Ensure-VerifiedShard([hashtable]$Shard) {
    $target = Join-Path $downloads $Shard.Name
    if (Test-Path $target) {
        $existingHash = Get-Sha256 $target
        if ($existingHash -eq $Shard.Sha256.ToLowerInvariant()) {
            Write-Host "Using verified existing download: $target"
            return $target
        }
        Write-Warning "Existing $($Shard.Name) failed SHA-256. Deleting the incomplete/corrupt TEST download and downloading it again."
        Remove-Item -Force $target
    }

    Download-Fresh $Shard.Url $target
    $actual = Get-Sha256 $target
    if ($actual -ne $Shard.Sha256.ToLowerInvariant()) {
        Remove-Item -Force $target -ErrorAction SilentlyContinue
        throw "SHA-256 mismatch after fresh download for $target`nexpected=$($Shard.Sha256)`nactual=$actual"
    }
    Write-Host "SHA-256 OK: $($Shard.Name)"
    return $target
}

function Ensure-ResourcePack([hashtable]$Resource) {
    $target = Join-Path $downloads $Resource.Name
    if (Test-Path $target) {
        $existingSize = (Get-Item $target).Length
        if ($existingSize -eq $Resource.Size) {
            Write-Host "Using existing resources.zip with the official release size: $target"
            return $target
        }
        Write-Warning "Existing resources.zip has an unexpected size. Deleting the TEST download and downloading it again."
        Remove-Item -Force $target
    }

    Download-Fresh $Resource.Url $target
    $actualSize = (Get-Item $target).Length
    if ($actualSize -ne $Resource.Size) {
        Remove-Item -Force $target -ErrorAction SilentlyContinue
        throw "Unexpected resources.zip size after fresh download. expected=$($Resource.Size) actual=$actualSize"
    }
    Write-Host 'resources.zip size matches the official GitHub release metadata. No official release digest is published for this asset.'
    return $target
}

Require-Command 'curl.exe'
Require-Command 'tar.exe'

New-Item -ItemType Directory -Force -Path $Root | Out-Null
New-Item -ItemType Directory -Force -Path $downloads | Out-Null

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

$verifiedShardPaths = @()
foreach ($shard in $shards) {
    $verifiedShardPaths += Ensure-VerifiedShard $shard
}

$resourceTarget = $null
if (-not $SkipResourcePack) {
    $resourceTarget = Ensure-ResourcePack $resource
}

foreach ($path in $verifiedShardPaths) {
    Write-Host "Extracting $(Split-Path $path -Leaf) into the shared test output..."
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
Write-Host "Resource pack: $(if ($SkipResourcePack) { 'SKIPPED' } else { $resourceTarget })"
Write-Host ''
Write-Host 'NEXT: copy the detected save root into a Minecraft 26.2 TEST saves directory. Do not overwrite the original 1.20.1 source.'
Write-Host 'Then follow docs/34_DREHMAL_26_2_MIGRATION_AUDIT.md. This script does NOT claim Minecraft 26.2 migration success.'
