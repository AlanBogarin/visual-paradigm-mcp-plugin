param(
    [switch]$ConfigureCodex,
    [string]$PackageRoot = (Join-Path $PSScriptRoot 'target\visual-paradigm-mcp-plugin'),
    [string]$PluginDirectory = (Join-Path $env:APPDATA 'VisualParadigm\plugins\visual-paradigm-mcp-plugin'),
    [string]$CodexConfigPath,
    [ValidateRange(1, 65535)]
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
if (-not $PSBoundParameters.ContainsKey('PackageRoot') -and (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'plugin.xml'))) {
    $PackageRoot = $PSScriptRoot
}
$packageRoot = (Resolve-Path -LiteralPath $PackageRoot).Path
$pluginRoot = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($PluginDirectory)
$files = @('plugin.xml', 'plugin.properties', 'lib\visual-paradigm-mcp-plugin.jar')
foreach ($file in $files) {
    if (-not (Test-Path -LiteralPath (Join-Path $packageRoot $file))) {
        throw "Missing packaged file: $file. Run .\run.ps1 package first."
    }
}
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
New-Item -ItemType Directory -Path (Join-Path $pluginRoot 'lib') -Force | Out-Null
foreach ($file in $files) {
    $target = Join-Path $pluginRoot $file
    if (Test-Path -LiteralPath $target) {
        $backup = Join-Path $pluginRoot "backups\$stamp\$file"
        New-Item -ItemType Directory -Path (Split-Path -Parent $backup) -Force | Out-Null
        Copy-Item -LiteralPath $target -Destination $backup
    }
    Copy-Item -LiteralPath (Join-Path $packageRoot $file) -Destination $target -Force
}
Write-Output "Installed plugin: $pluginRoot"

if ($ConfigureCodex) {
    if ($CodexConfigPath) {
        $configPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($CodexConfigPath)
        $configRoot = Split-Path -Parent $configPath
    } else {
        $configRoot = if ($env:CODEX_HOME) { $env:CODEX_HOME } else { Join-Path $env:USERPROFILE '.codex' }
        $configPath = Join-Path $configRoot 'config.toml'
    }
    $config = if (Test-Path -LiteralPath $configPath) { [IO.File]::ReadAllText($configPath) } else { '' }
    if ($config -match '(?m)^\[mcp_servers\.visual_paradigm\]') {
        Write-Output 'Codex visual_paradigm entry already exists; inspect it before changing it.'
    } else {
        New-Item -ItemType Directory -Path $configRoot -Force | Out-Null
        if (Test-Path -LiteralPath $configPath) {
            Copy-Item -LiteralPath $configPath -Destination "$configPath.backup-$stamp"
        }
        $entry = @'

[mcp_servers.visual_paradigm]
url = "http://127.0.0.1:8080/mcp"
startup_timeout_sec = 20
tool_timeout_sec = 60

'@
        $entry = $entry.Replace('http://127.0.0.1:8080/mcp', "http://127.0.0.1:$Port/mcp")
        [IO.File]::WriteAllText($configPath, $config.TrimEnd() + "`n" + $entry, [Text.UTF8Encoding]::new($false))
        Write-Output "Configured Codex: $configPath"
    }
}
Write-Output 'Restart Visual Paradigm and reload MCP servers in Codex to use the plugin.'
