# Builds a self-contained Windows app (no JVM install required for the end user).
#
# Prereqs on the build machine (not on the user's machine):
#   - JDK 17+ on PATH (provides both `mvn` invocations via JAVA_HOME and `jpackage`).
#   - Maven on PATH.
#
# Output:
#   <repo root>/Guest List Cleaner-<version>.exe  -- Windows installer.
#     Ship this single file.

$ErrorActionPreference = 'Stop'

# Resolve jpackage. PATH usually only has Oracle's javapath shim (java/javac
# only), so we look in JAVA_HOME first, then common JDK install locations.
function Resolve-Jpackage {
    $cmd = Get-Command jpackage -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Path }

    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME 'bin\jpackage.exe') }
    $candidates += Get-ChildItem 'C:\Program Files\Java' -Directory -ErrorAction SilentlyContinue |
        ForEach-Object { Join-Path $_.FullName 'bin\jpackage.exe' }
    $candidates += Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
        ForEach-Object { Join-Path $_.FullName 'bin\jpackage.exe' }

    foreach ($p in $candidates) {
        if (Test-Path $p) { return $p }
    }
    throw 'Could not find jpackage.exe. Set JAVA_HOME to a JDK 14+ install, or add its bin/ to PATH.'
}
$jpackage = Resolve-Jpackage
Write-Host "Using jpackage: $jpackage" -ForegroundColor DarkGray

# Auto-bump the installer version. installer-version.txt holds the version
# this script will USE for the build it's about to run; we increment the patch
# segment first so each release ships a unique, increasing version (MSI rejects
# upgrades to the same or older version).
$versionFile = Join-Path $PSScriptRoot 'installer-version.txt'
if (-not (Test-Path $versionFile)) { '1.0.0' | Set-Content $versionFile -Encoding utf8 }
$parts = (Get-Content $versionFile -Raw).Trim().Split('.')
if ($parts.Count -ne 3) { throw "installer-version.txt must be MAJOR.MINOR.PATCH, got: $($parts -join '.')" }
$parts[2] = [int]$parts[2] + 1
$appVersion = $parts -join '.'
$appVersion | Set-Content $versionFile -Encoding utf8
Write-Host "Building version $appVersion" -ForegroundColor Cyan

$jarName       = 'guest-list-cleaner-1.0.0-SNAPSHOT.jar'
$appName       = 'Guest List Cleaner'
$stage         = 'target/jpackage-input'
$dest          = $PSScriptRoot               # repo root (folder this script lives in)
$installerName = "$appName-$appVersion.exe"  # jpackage names the file this way

Write-Host 'Building shaded jar...' -ForegroundColor Cyan
mvn -q clean package
if ($LASTEXITCODE -ne 0) { throw "Maven build failed (exit $LASTEXITCODE)" }

Write-Host 'Staging jar for jpackage...' -ForegroundColor Cyan
if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }
New-Item -ItemType Directory -Path $stage | Out-Null
Copy-Item "target/$jarName" "$stage/$jarName"

# Clear any previous installer files at the dest (any version), so old builds
# don't accumulate next to the new one. Never touch the dest folder itself
# (which is the repo root).
Get-ChildItem -Path $dest -Filter "$appName-*.exe" -File -ErrorAction SilentlyContinue |
    Remove-Item -Force

Write-Host 'Running jpackage...' -ForegroundColor Cyan
& $jpackage `
    --type exe `
    --input $stage `
    --main-jar $jarName `
    --main-class App `
    --name $appName `
    --app-version $appVersion `
    --vendor 'Jose Silva' `
    --dest $dest `
    --win-per-user-install `
    --win-menu `
    --win-menu-group $appName `
    --win-shortcut `
    --icon 'assets/app.ico' `
    --license-file 'assets/LICENSE.txt'
if ($LASTEXITCODE -ne 0) { throw "jpackage failed (exit $LASTEXITCODE)" }

Write-Host "Done. Installer at: $(Join-Path $dest $installerName)" -ForegroundColor Green
