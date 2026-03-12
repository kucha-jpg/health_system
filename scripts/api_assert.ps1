param(
  [string]$BaseUrl = "http://127.0.0.1:9090/api"
)

$ErrorActionPreference = "Stop"

powershell -ExecutionPolicy Bypass -File "$PSScriptRoot\api_test.ps1" -BaseUrl $BaseUrl -AssertOnly
exit $LASTEXITCODE
