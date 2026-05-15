param(
  [string]$BaseUrl = "http://127.0.0.1:9090/api",
  [string]$AdminUser = "admin",
  [string]$AdminPass = "123456",
  [string]$DoctorUser = "doctor_demo_01",
  [string]$DoctorPass = "123456"
)

$ErrorActionPreference = "Continue"
$pass = 0
$fail = 0

# indicator type for blood pressure (cannot use literal Chinese in this file encoding)
$BP = [char]0x8840 + [char]0x538b

# unique username per run (avoid 409 on re-run)
$SMOKE_USER = "smoke_" + (Get-Date -Format "HHmmss")

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Health System - 12 API Smoke Tests" -ForegroundColor Cyan
Write-Host "  BaseUrl: $BaseUrl" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# ============================================================
# helper functions
# ============================================================
function ApiPost($Path, $Payload, $Token) {
  $headers = @{ "Content-Type" = "application/json; charset=utf-8" }
  if ($Token) { $headers["Authorization"] = "Bearer $Token" }
  $json = $Payload | ConvertTo-Json -Depth 8 -Compress
  $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($json)
  try {
    $res = Invoke-RestMethod -Method Post -Uri "$BaseUrl$Path" -Headers $headers -Body $bodyBytes -ErrorAction Stop
    return @{ code = [int]$res.code; body = $res; httpStatus = 200 }
  } catch {
    $sc = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode.value__ } else { -1 }
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $raw = $reader.ReadToEnd()
    $c = 0
    try { $c = [int]($raw | ConvertFrom-Json).code } catch { $c = $sc }
    return @{ code = $c; body = $raw; httpStatus = $sc }
  }
}

function ApiGet($Path, $Token) {
  $headers = @{}
  if ($Token) { $headers["Authorization"] = "Bearer $Token" }
  try {
    $res = Invoke-RestMethod -Method Get -Uri "$BaseUrl$Path" -Headers $headers -ErrorAction Stop
    return @{ code = [int]$res.code; body = $res; httpStatus = 200 }
  } catch {
    $sc = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode.value__ } else { -1 }
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $raw = $reader.ReadToEnd()
    $c = 0
    try { $c = [int]($raw | ConvertFrom-Json).code } catch { $c = $sc }
    return @{ code = $c; body = $raw; httpStatus = $sc }
  }
}

function ExtractToken($resp) {
  if ($resp.body -and $resp.body.data -and $resp.body.data.token) {
    return $resp.body.data.token
  }
  return ""
}

# ============================================================
function Test-Case($num, $desc, $expectedCode, $actualCode, $detail) {
  $prefix = "[CASE-$num]"
  if ($actualCode -eq $expectedCode) {
    Write-Host "$prefix PASS  $desc" -ForegroundColor Green
    $script:pass++
  } else {
    Write-Host "$prefix FAIL  $desc" -ForegroundColor Red
    Write-Host "        expected code=$expectedCode, actual code=$actualCode" -ForegroundColor Red
    if ($detail) { Write-Host "        $detail" -ForegroundColor DarkYellow }
    $script:fail++
  }
}

# ============================================================
# 3.1 Auth
# ============================================================
Write-Host "--- 3.1 Auth ---" -ForegroundColor Yellow

$r1 = ApiPost "/auth/login" @{ username = $AdminUser; password = $AdminPass }
$adminToken = ExtractToken $r1
Test-Case 1 "admin login (200)" 200 $r1.code ""

$r2 = ApiPost "/auth/login" @{ username = $AdminUser; password = "wrong" }
Test-Case 2 "wrong password -> 401" 401 $r2.code "msg: $($r2.body)"

$r3 = ApiPost "/auth/register" @{ username = $SMOKE_USER; password = "123456"; name = "SmokeTest"; phone = "13800001111" }
$patientToken = ExtractToken $r3
Test-Case 3 "register patient (200)" 200 $r3.code ""

# ============================================================
# 3.2 Patient
# ============================================================
Write-Host "`n--- 3.2 Patient ---" -ForegroundColor Yellow

if ($patientToken) {
  $r4 = ApiPost "/patient/archive" @{ name = "SmokeTest"; age = 35; medicalHistory = "none" } $patientToken
  Test-Case 4 "save archive (200)" 200 $r4.code ""

  $r5 = ApiPost "/patient/data" @{ indicatorType = $BP; value = "190/120" } $patientToken
  Test-Case 5 "report high BP (200)" 200 $r5.code ""

  $r6 = ApiGet "/patient/alerts?status=OPEN" $patientToken
  Test-Case 6 "query alerts (200)" 200 $r6.code "total: $($r6.body.data.total)"
} else {
  Test-Case 4 "save archive (SKIP)" 0 0 "patientToken missing"
  Test-Case 5 "report high BP (SKIP)" 0 0 "patientToken missing"
  Test-Case 6 "query alerts (SKIP)" 0 0 "patientToken missing"
}

# ============================================================
# 3.3 Doctor
# ============================================================
Write-Host "`n--- 3.3 Doctor ---" -ForegroundColor Yellow

$r7 = ApiPost "/auth/login" @{ username = $DoctorUser; password = $DoctorPass }
$doctorToken = ExtractToken $r7
Test-Case 7 "doctor login (200)" 200 $r7.code "user=$DoctorUser"

if ($doctorToken) {
  $r8 = ApiGet "/doctor/alerts?riskLevel=HIGH" $doctorToken
  Test-Case 8 "alert workbench (200)" 200 $r8.code ""

  $r9 = ApiGet "/doctor/groups" $doctorToken
  Test-Case 9 "group list (200)" 200 $r9.code "groups: $($r9.body.data.Count)"
} else {
  Test-Case 8 "alert workbench (SKIP)" 0 0 "doctorToken missing"
  Test-Case 9 "group list (SKIP)" 0 0 "doctorToken missing"
}

# ============================================================
# 3.4 Admin
# ============================================================
Write-Host "`n--- 3.4 Admin ---" -ForegroundColor Yellow

if ($adminToken) {
  $r10 = ApiGet "/admin/monitor/overview" $adminToken
  Test-Case 10 "monitor overview (200)" 200 $r10.code "users: $($r10.body.data.totalUsers)"

  $r11 = ApiGet "/admin/config/alert-rules" $adminToken
  Test-Case 11 "alert rules (200)" 200 $r11.code "rules: $($r11.body.data.Count)"

  $r12 = ApiGet "/admin/groups" $adminToken
  Test-Case 12 "group governance (200)" 200 $r12.code "total: $($r12.body.data.total)"
} else {
  Test-Case 10 "monitor overview (SKIP)" 0 0 "adminToken missing"
  Test-Case 11 "alert rules (SKIP)" 0 0 "adminToken missing"
  Test-Case 12 "group governance (SKIP)" 0 0 "adminToken missing"
}

# ============================================================
# summary
# ============================================================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  PASS: $pass  |  FAIL: $fail  |  TOTAL: 12" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($fail -gt 0) { exit 1 } else { exit 0 }
