param(
    [ValidateSet('build', 'test', 'package', 'format', 'validate', 'all')]
    [string]$Command = 'package',
    [string]$VpHome = 'C:\Program Files\Visual Paradigm CE 18.1',
    [string]$Maven = 'mvn.cmd'
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath (Join-Path $VpHome 'lib\openapi.jar'))) {
    throw "Visual Paradigm openapi.jar was not found in $VpHome"
}
if (-not (Get-Command $Maven -ErrorAction SilentlyContinue)) {
    $bundledMaven = Get-ChildItem 'C:\ProgramData\chocolatey\lib\maven\apache-maven-*\bin\mvn.cmd' -ErrorAction SilentlyContinue |
        Select-Object -Last 1
    if (-not $bundledMaven) { throw 'Install Maven or pass -Maven with the full path to mvn.cmd.' }
    $Maven = $bundledMaven.FullName
}
$goals = @(switch ($Command) {
    'build' { @('compile') }
    'test' { @('test') }
    'package' { @('package') }
    'format' { @('com.coveo:fmt-maven-plugin:2.9:format') }
    'validate' { @('validate') }
    'all' { @('verify') }
})
Push-Location $PSScriptRoot
try {
    & $Maven '--batch-mode' '-U' "-Dmaven.repo.local=$PSScriptRoot\.m2" "-Dvp.lib.dir=$VpHome\lib" @goals
    if ($LASTEXITCODE -ne 0) { throw "Maven failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}
