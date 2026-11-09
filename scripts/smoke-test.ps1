param([string]$ApiBase = "http://127.0.0.1:8080", [string]$ProviderBase = "http://127.0.0.1:8081")
$ErrorActionPreference = "Stop"
foreach ($base in @($ApiBase, $ProviderBase)) {
    $health = Invoke-RestMethod -Uri "$base/actuator/health" -TimeoutSec 10
    if ($health.status -ne "UP") { throw "Service is not healthy: $base" }
}
$result = Invoke-RestMethod -Uri "$ApiBase/api/system/user-service" -TimeoutSec 10
if ($result.code -ne "OK" -or $result.data.databaseProduct -ne "MySQL" -or
    $result.data.database -ne "live_platform" -or $result.data.probeValue -ne "stage01-ready" -or
    $result.data.serviceName -ne "live-user-provider" -or -not $result.requestId) {
    throw "RPC/database probe did not return the expected result."
}
$result | ConvertTo-Json -Depth 5
Write-Host "PASS: HTTP -> Dubbo -> user provider -> MySQL"
