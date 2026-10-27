param(
    [uri]$Endpoint = 'http://127.0.0.1:8080/mcp',
    [switch]$CreateExample,
    [string]$DiagramName = 'MCP demo - Online shop'
)

$ErrorActionPreference = 'Stop'
$requestId = 0
$headers = @{ Accept = 'application/json, text/event-stream' }
function Invoke-McpRequest {
    param([string]$Method, [hashtable]$Params)
    $script:requestId++
    $request = @{ jsonrpc = '2.0'; id = $script:requestId; method = $Method; params = $Params }
    $body = [Text.Encoding]::UTF8.GetBytes(($request | ConvertTo-Json -Depth 12 -Compress))
    $response = Invoke-RestMethod -Uri $Endpoint -Method Post -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $body -TimeoutSec 30
    if ($response.error) { throw ($response.error | ConvertTo-Json -Compress) }
    if ($response.id -ne $script:requestId) { throw 'Unexpected JSON-RPC response ID.' }
    return $response.result
}
function Invoke-McpTool {
    param([string]$Name, [hashtable]$Arguments)
    $result = Invoke-McpRequest 'tools/call' @{ name = $Name; arguments = $Arguments }
    $message = ($result.content | ForEach-Object { $_.text }) -join [Environment]::NewLine
    if ($result.isError) { throw "$Name failed: $message" }
    Write-Output $message
}

$connection = Invoke-McpRequest 'initialize' @{
    protocolVersion = '2025-06-18'
    capabilities = @{}
    clientInfo = @{ name = 'visual-paradigm-fork-smoke-test'; version = '1.0' }
}
$headers['MCP-Protocol-Version'] = $connection.protocolVersion
$notification = [Text.Encoding]::UTF8.GetBytes('{"jsonrpc":"2.0","method":"notifications/initialized"}')
Invoke-WebRequest -Uri $Endpoint -Method Post -Headers $headers -ContentType 'application/json' -Body $notification -TimeoutSec 15 -UseBasicParsing | Out-Null
$null = Invoke-McpRequest 'ping' @{}
$catalog = Invoke-McpRequest 'tools/list' @{}
$requiredTools = @('createUseCaseDiagram', 'addActor', 'addUseCase', 'addRelationship', 'generateReport')
foreach ($name in $requiredTools) {
    if ($name -notin $catalog.tools.name) { throw "Missing MCP tool: $name" }
}
Write-Output "Connected: $($connection.serverInfo.name) ($($connection.protocolVersion))"
Write-Output "Available tools: $($catalog.tools.name -join ', ')"
if (-not $CreateExample) {
    Write-Output 'Connection verified. No project changes were requested.'
    return
}

Invoke-McpTool 'createUseCaseDiagram' @{ diagramName = $DiagramName }
Invoke-McpTool 'addActor' @{ actorName = 'Customer'; diagramName = $DiagramName }
foreach ($name in @('Place order', 'Validate payment', 'Apply coupon')) {
    Invoke-McpTool 'addUseCase' @{ useCaseName = $name; diagramName = $DiagramName }
}
Invoke-McpTool 'addRelationship' @{ actorName = 'Customer'; useCaseName = 'Place order'; relationshipType = 'Association'; diagramName = $DiagramName }
Invoke-McpTool 'addRelationship' @{ actorName = 'Place order'; useCaseName = 'Validate payment'; relationshipType = 'Include'; diagramName = $DiagramName }
Invoke-McpTool 'addRelationship' @{ actorName = 'Apply coupon'; useCaseName = 'Place order'; relationshipType = 'Extend'; diagramName = $DiagramName }
Invoke-McpTool 'generateReport' @{ diagramName = $DiagramName }
Write-Output 'Example created in the open project. Save it in Visual Paradigm to persist changes.'
