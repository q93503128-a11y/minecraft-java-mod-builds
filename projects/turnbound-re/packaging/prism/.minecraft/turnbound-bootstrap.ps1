param(
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$WorldVersion = '2.2.2f'
$ProfileId = 'turnbound_re:drehmal_apotheosis_2_2_2f'
$ExpectedMapHash = '2e6232dc3e97c77eaa006b09e3ee09b246c46e3df68359dcc1495ce19d1e8053'
$WorldFolderName = 'TURNBOUND RE - Drehmal APOTHEOSIS 2.2.2f'
$ResourcePackName = 'Drehmal Resource Pack v2.2.2f.zip'
$ShardUrls = @(
    'https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/shard_1.zip',
    'https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/shard_2.zip',
    'https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/shard_3.zip'
)
$ResourcePackUrl = 'https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/resources.zip'

function Assert-PinnedManifest {
    if ($ShardUrls.Count -ne 3) {
        throw 'TURNBOUND bootstrap expects exactly three pinned Drehmal map shards.'
    }
    if ($ExpectedMapHash -notmatch '^[0-9a-f]{64}$') {
        throw 'Pinned Drehmal directory SHA-256 is malformed.'
    }
    foreach ($url in ($ShardUrls + $ResourcePackUrl)) {
        if (-not $url.StartsWith('https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/')) {
            throw "Unexpected external download origin: $url"
        }
    }
}

function Test-ZipArchive([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $false
    }
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
        $zip.Dispose()
        return $true
    } catch {
        return $false
    }
}

function Download-ZipAtomic([string]$Url, [string]$Destination) {
    if (Test-ZipArchive $Destination) {
        Write-Host "Using cached archive: $Destination"
        return
    }

    $parent = Split-Path -Parent $Destination
    [System.IO.Directory]::CreateDirectory($parent) | Out-Null
    $partial = "$Destination.part"
    Remove-Item -LiteralPath $partial -Force -ErrorAction SilentlyContinue

    Write-Host "Downloading: $Url"
    $curl = Get-Command 'curl.exe' -ErrorAction SilentlyContinue
    if ($null -ne $curl) {
        & $curl.Source --location --fail --retry 4 --retry-delay 3 --output $partial $Url
        if ($LASTEXITCODE -ne 0) {
            throw "curl failed with exit code $LASTEXITCODE while downloading $Url"
        }
    } else {
        Invoke-WebRequest -Uri $Url -OutFile $partial -UseBasicParsing
    }

    if (-not (Test-ZipArchive $partial)) {
        Remove-Item -LiteralPath $partial -Force -ErrorAction SilentlyContinue
        throw "Downloaded archive is not a readable ZIP: $Url"
    }

    Move-Item -LiteralPath $partial -Destination $Destination -Force
}

function Expand-ZipMerged([string]$ArchivePath, [string]$Destination) {
    $root = [System.IO.Path]::GetFullPath($Destination)
    if (-not $root.EndsWith([System.IO.Path]::DirectorySeparatorChar.ToString())) {
        $root += [System.IO.Path]::DirectorySeparatorChar
    }

    $zip = [System.IO.Compression.ZipFile]::OpenRead($ArchivePath)
    try {
        foreach ($entry in $zip.Entries) {
            $relative = $entry.FullName.Replace('/', [System.IO.Path]::DirectorySeparatorChar)
            $target = [System.IO.Path]::GetFullPath((Join-Path $Destination $relative))
            if (-not $target.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
                throw "Unsafe ZIP entry rejected: $($entry.FullName)"
            }

            if ([string]::IsNullOrEmpty($entry.Name)) {
                [System.IO.Directory]::CreateDirectory($target) | Out-Null
                continue
            }

            $targetParent = [System.IO.Path]::GetDirectoryName($target)
            if (-not [string]::IsNullOrWhiteSpace($targetParent)) {
                [System.IO.Directory]::CreateDirectory($targetParent) | Out-Null
            }

            $input = $entry.Open()
            $output = [System.IO.File]::Open(
                $target,
                [System.IO.FileMode]::Create,
                [System.IO.FileAccess]::Write,
                [System.IO.FileShare]::None
            )
            try {
                $input.CopyTo($output)
            } finally {
                $output.Dispose()
                $input.Dispose()
            }
        }
    } finally {
        $zip.Dispose()
    }
}

function Get-DirectoryContentHash([string]$Directory) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $items = [System.IO.Directory]::GetFileSystemEntries($Directory)
        [System.Array]::Sort($items, [System.StringComparer]::Ordinal)
        foreach ($item in $items) {
            if ([System.IO.Directory]::Exists($item)) {
                $childHash = Get-DirectoryContentHash $item
                $bytes = [System.Text.Encoding]::UTF8.GetBytes($childHash)
                [void]$sha.TransformBlock($bytes, 0, $bytes.Length, $bytes, 0)
                continue
            }

            $stream = [System.IO.File]::OpenRead($item)
            try {
                $buffer = New-Object byte[] (1024 * 1024)
                while (($read = $stream.Read($buffer, 0, $buffer.Length)) -gt 0) {
                    [void]$sha.TransformBlock($buffer, 0, $read, $buffer, 0)
                }
            } finally {
                $stream.Dispose()
            }
        }

        [void]$sha.TransformFinalBlock([byte[]]::new(0), 0, 0)
        return ([System.BitConverter]::ToString($sha.Hash)).Replace('-', '').ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
}

function Set-OptionValue([string]$OptionsPath, [string]$Key, [string]$Value) {
    $lines = @()
    if (Test-Path -LiteralPath $OptionsPath -PathType Leaf) {
        $lines = @(Get-Content -LiteralPath $OptionsPath)
    }

    $prefix = "$Key`:"
    $found = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i].StartsWith($prefix, [System.StringComparison]::Ordinal)) {
            $lines[$i] = "$Key`:$Value"
            $found = $true
            break
        }
    }
    if (-not $found) {
        $lines += "$Key`:$Value"
    }

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($OptionsPath, [string[]]$lines, $utf8NoBom)
}

function Enable-DrehmalResourcePack([string]$OptionsPath) {
    $entry = "file/$ResourcePackName"
    $packs = New-Object System.Collections.Generic.List[string]

    if (Test-Path -LiteralPath $OptionsPath -PathType Leaf) {
        foreach ($line in Get-Content -LiteralPath $OptionsPath) {
            if ($line.StartsWith('resourcePacks:', [System.StringComparison]::Ordinal)) {
                $raw = $line.Substring('resourcePacks:'.Length)
                try {
                    foreach ($pack in @($raw | ConvertFrom-Json)) {
                        if ($null -ne $pack -and -not $packs.Contains([string]$pack)) {
                            $packs.Add([string]$pack)
                        }
                    }
                } catch {
                    Write-Warning 'Existing resourcePacks option could not be parsed; rebuilding the list.'
                }
                break
            }
        }
    }

    if (-not $packs.Contains('vanilla')) {
        $packs.Insert(0, 'vanilla')
    }
    if (-not $packs.Contains($entry)) {
        $packs.Add($entry)
    }

    $json = ConvertTo-Json -Compress -InputObject ([string[]]$packs.ToArray())
    Set-OptionValue $OptionsPath 'resourcePacks' $json

    $currentScale = $null
    if (Test-Path -LiteralPath $OptionsPath -PathType Leaf) {
        foreach ($line in Get-Content -LiteralPath $OptionsPath) {
            if ($line.StartsWith('guiScale:', [System.StringComparison]::Ordinal)) {
                [int]$parsed = 0
                if ([int]::TryParse($line.Substring('guiScale:'.Length), [ref]$parsed)) {
                    $currentScale = $parsed
                }
                break
            }
        }
    }
    if ($null -eq $currentScale -or $currentScale -gt 3) {
        Set-OptionValue $OptionsPath 'guiScale' '3'
    }
}

Assert-PinnedManifest
Add-Type -AssemblyName System.IO.Compression.FileSystem

if ($ValidateOnly) {
    Write-Host "TURNBOUND bootstrap manifest OK: Drehmal $WorldVersion / $ProfileId"
    exit 0
}

$mcDir = $env:INST_MC_DIR
if ([string]::IsNullOrWhiteSpace($mcDir)) {
    $mcDir = $PSScriptRoot
}
$mcDir = [System.IO.Path]::GetFullPath($mcDir)

$worldDir = Join-Path (Join-Path $mcDir 'saves') $WorldFolderName
$profileMarker = Join-Path $worldDir '.turnbound_re_profile'
$cacheDir = Join-Path $mcDir ".turnbound-re-cache\$WorldVersion"
$installingFlag = Join-Path $cacheDir 'installing.flag'
$resourcePackDir = Join-Path $mcDir 'resourcepacks'
$resourcePackPath = Join-Path $resourcePackDir $ResourcePackName
$optionsPath = Join-Path $mcDir 'options.txt'

[System.IO.Directory]::CreateDirectory((Split-Path -Parent $worldDir)) | Out-Null
[System.IO.Directory]::CreateDirectory($cacheDir) | Out-Null
[System.IO.Directory]::CreateDirectory($resourcePackDir) | Out-Null

$worldReady = (Test-Path -LiteralPath (Join-Path $worldDir 'level.dat') -PathType Leaf) -and (Test-Path -LiteralPath $profileMarker -PathType Leaf) -and ((Get-Content -LiteralPath $profileMarker -Raw).Trim() -eq $ProfileId)

if (-not $worldReady) {
    if ((Test-Path -LiteralPath $worldDir -PathType Container) -and
        (Test-Path -LiteralPath $installingFlag -PathType Leaf)) {
        Write-Host 'Previous TURNBOUND world bootstrap was interrupted; rebuilding only the dedicated TURNBOUND save.'
        Remove-Item -LiteralPath $worldDir -Recurse -Force
    } elseif (Test-Path -LiteralPath $worldDir -PathType Container) {
        if (Test-Path -LiteralPath (Join-Path $worldDir 'level.dat') -PathType Leaf) {
            Write-Host 'Existing unmarked TURNBOUND save found; validating before using it.'
            $existingHash = Get-DirectoryContentHash $worldDir
            if ($existingHash -eq $ExpectedMapHash) {
                [System.IO.File]::WriteAllText($profileMarker, "$ProfileId`n", (New-Object System.Text.UTF8Encoding($false)))
                $worldReady = $true
            } else {
                throw "Existing save '$WorldFolderName' is not the pinned clean Drehmal $WorldVersion world. It was left untouched."
            }
        } else {
            throw "Existing directory '$WorldFolderName' is not a complete Minecraft save. It was left untouched."
        }
    }

    if (-not $worldReady) {
        $driveRoot = [System.IO.Path]::GetPathRoot($mcDir)
        $drive = New-Object System.IO.DriveInfo($driveRoot)
        if ($drive.AvailableFreeSpace -lt 12GB) {
            throw 'TURNBOUND first-run setup needs at least 12 GiB of free disk space for the map download and extraction.'
        }

        [System.IO.File]::WriteAllText($installingFlag, "installing $WorldVersion`n")
        [System.IO.Directory]::CreateDirectory($worldDir) | Out-Null

        for ($i = 0; $i -lt $ShardUrls.Count; $i++) {
            $shardPath = Join-Path $cacheDir ("shard_{0}.zip" -f ($i + 1))
            Download-ZipAtomic $ShardUrls[$i] $shardPath
        }

        for ($i = 0; $i -lt $ShardUrls.Count; $i++) {
            $shardPath = Join-Path $cacheDir ("shard_{0}.zip" -f ($i + 1))
            Write-Host ("Extracting Drehmal map shard {0}/{1}..." -f ($i + 1), $ShardUrls.Count)
            Expand-ZipMerged $shardPath $worldDir
        }

        if (-not (Test-Path -LiteralPath (Join-Path $worldDir 'level.dat') -PathType Leaf)) {
            throw 'Drehmal shard extraction completed without level.dat.'
        }

        Write-Host 'Validating the complete pinned Drehmal directory hash. This can take a while on first install...'
        $mapHash = Get-DirectoryContentHash $worldDir
        if ($mapHash -ne $ExpectedMapHash) {
            throw "Drehmal map validation failed. Expected $ExpectedMapHash but got $mapHash. Cached shards were preserved for retry."
        }

        [System.IO.File]::WriteAllText($profileMarker, "$ProfileId`n", (New-Object System.Text.UTF8Encoding($false)))
        $worldReady = $true
    }
}

Download-ZipAtomic $ResourcePackUrl $resourcePackPath
Enable-DrehmalResourcePack $optionsPath

if (-not $worldReady) {
    throw 'TURNBOUND external world bootstrap did not reach the ready state.'
}

Remove-Item -LiteralPath $installingFlag -Force -ErrorAction SilentlyContinue
foreach ($i in 1..3) {
    Remove-Item -LiteralPath (Join-Path $cacheDir ("shard_{0}.zip" -f $i)) -Force -ErrorAction SilentlyContinue
}

Write-Host ''
Write-Host 'TURNBOUND: RE first-run setup is ready.'
Write-Host "World: $WorldFolderName"
Write-Host 'The mod will bind the verified world automatically when you enter it.'
