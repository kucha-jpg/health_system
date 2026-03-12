param(
  [string]$BaseUrl = "http://127.0.0.1:9090/api",
  [string]$PatientUser = "patient_test_01",
  [string]$PatientPass = "123456",
  [string]$PatientPhone = "13800009999",
  [string]$PatientName = "patient test",
  [string]$AdminUser = "admin",
  [string]$AdminPass = "123456",
  [string]$DoctorUser = "doctor_demo_01",
  [string]$DoctorPass = "123456",
  [switch]$AssertOnly
)

$ErrorActionPreference = "Stop"
$script:AssertFailed = 0

function Write-Title([string]$title) {
  Write-Host "`n==== $title ===="
}

function Invoke-ApiPost([string]$Path, [hashtable]$Payload, [string]$Token = "") {
  $headers = @{ "Content-Type" = "application/json" }
  if ($Token) {
    $headers["Authorization"] = "Bearer $Token"
  }
  return Invoke-RestMethod -Method Post -Uri ("$BaseUrl$Path") -Headers $headers -Body ($Payload | ConvertTo-Json)
}

function Invoke-ApiGetRaw([string]$Path, [string]$Token = "") {
  $headers = @{}
  if ($Token) {
    $headers["Authorization"] = "Bearer $Token"
  }

  try {
    $resp = Invoke-WebRequest -Method Get -Uri ("$BaseUrl$Path") -Headers $headers -ErrorAction Stop
    return @{ status = [int]$resp.StatusCode; body = $resp.Content }
  } catch {
    if ($_.Exception.Response) {
      $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
      $body = $reader.ReadToEnd()
      return @{ status = [int]$_.Exception.Response.StatusCode.value__; body = $body }
    }
    throw
  }
}

function Invoke-ApiPostRaw([string]$Path, [hashtable]$Payload, [string]$Token = "") {
  $headers = @{ "Content-Type" = "application/json" }
  if ($Token) {
    $headers["Authorization"] = "Bearer $Token"
  }
  try {
    $resp = Invoke-WebRequest -Method Post -Uri ("$BaseUrl$Path") -Headers $headers -Body ($Payload | ConvertTo-Json -Depth 8) -ErrorAction Stop
    return @{ status = [int]$resp.StatusCode; body = $resp.Content }
  } catch {
    if ($_.Exception.Response) {
      $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
      $body = $reader.ReadToEnd()
      return @{ status = [int]$_.Exception.Response.StatusCode.value__; body = $body }
    }
    throw
  }
}

function Invoke-ApiPutRaw([string]$Path, [hashtable]$Payload, [string]$Token = "") {
  $headers = @{ "Content-Type" = "application/json" }
  if ($Token) {
    $headers["Authorization"] = "Bearer $Token"
  }
  try {
    $resp = Invoke-WebRequest -Method Put -Uri ("$BaseUrl$Path") -Headers $headers -Body ($Payload | ConvertTo-Json -Depth 8) -ErrorAction Stop
    return @{ status = [int]$resp.StatusCode; body = $resp.Content }
  } catch {
    if ($_.Exception.Response) {
      $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
      $body = $reader.ReadToEnd()
      return @{ status = [int]$_.Exception.Response.StatusCode.value__; body = $body }
    }
    return @{ status = -1; body = "" }
  }
}

function Get-BodyCode([string]$RawBody) {
  if ([string]::IsNullOrWhiteSpace($RawBody)) { return "" }
  try {
    $obj = $RawBody | ConvertFrom-Json
    return [string]$obj.code
  } catch {
    return ""
  }
}

function Assert-Equal([string]$Label, [string]$Expected, [string]$Actual) {
  if ($Expected -eq $Actual) {
    Write-Host "[ASSERT-OK] $Label expected=$Expected actual=$Actual"
    return
  }
  $script:AssertFailed++
  Write-Host "[ASSERT-FAIL] $Label expected=$Expected actual=$Actual"
}

function Get-Token($resp) {
  if ($null -eq $resp -or $null -eq $resp.data) { return "" }
  return [string]$resp.data.token
}

Write-Host "[INFO] BASE_URL=$BaseUrl"
Write-Host "[INFO] ASSERT_ONLY=$($AssertOnly.IsPresent)"
$indicatorBloodPressure = [string]([char]0x8840) + [char]0x538b

$doctorToken2 = ""

if (-not $AssertOnly) {
  Write-Title "CASE-1 patient register -> login -> report -> list"
  try {
    $register = Invoke-ApiPost "/auth/register" @{
      username = $PatientUser
      password = $PatientPass
      phone = $PatientPhone
      name = $PatientName
    }
    Write-Host "[REGISTER]" ($register | ConvertTo-Json -Depth 6)
  } catch {
    Write-Host "[REGISTER] skipped/error:" $_.Exception.Message
  }

  $patientLogin = Invoke-ApiPost "/auth/login" @{ username = $PatientUser; password = $PatientPass }
  Write-Host "[LOGIN]" ($patientLogin | ConvertTo-Json -Depth 6)
  $patientToken = Get-Token $patientLogin
  $patientUserId = $null
  if ($patientLogin -and $patientLogin.data -and $patientLogin.data.userInfo) {
    $patientUserId = $patientLogin.data.userInfo.id
  }

  if ($patientToken) {
    $reportHeaders = @{ "Content-Type" = "application/json; charset=utf-8"; "Authorization" = "Bearer $patientToken" }
    $reportPayload = '{"indicatorType":"\u8840\u538b","value":"135/88","remark":"api test"}'
    $report = Invoke-RestMethod -Method Post -Uri ("$BaseUrl/patient/data") -Headers $reportHeaders -Body $reportPayload
    Write-Host "[REPORT]" ($report | ConvertTo-Json -Depth 6)
    $list = Invoke-ApiGetRaw "/patient/data?indicator_type=%E8%A1%80%E5%8E%8B&timeRange=week" $patientToken
    Write-Host "[LIST] status=$($list.status)"
    Write-Host "[LIST-BODY] $($list.body)"
  } else {
    Write-Host "[WARN] patient token missing, CASE-1 partial"
  }

  Write-Title "CASE-2 patient abnormal report -> doctor alerts"
  $adminLogin = Invoke-ApiPost "/auth/login" @{ username = $AdminUser; password = $AdminPass }
  $adminToken = Get-Token $adminLogin
  $doctorLogin = Invoke-ApiPost "/auth/login" @{ username = $DoctorUser; password = $DoctorPass }
  $doctorToken = Get-Token $doctorLogin
  if (-not $adminToken) {
    Write-Host "[WARN] admin login failed, stop CASE-2"
  } else {
    if ($patientToken) {
      $alertHeaders = @{ "Content-Type" = "application/json; charset=utf-8"; "Authorization" = "Bearer $patientToken" }
      $alertPayload = '{"indicatorType":"\u8840\u538b","value":"190/120","remark":"alert case"}'
      $alertCase = Invoke-RestMethod -Method Post -Uri ("$BaseUrl/patient/data") -Headers $alertHeaders -Body $alertPayload
      Write-Host "[ALERT_REPORT]" ($alertCase | ConvertTo-Json -Depth 6)
      if ($doctorToken) {
        $doctorAlerts = Invoke-ApiGetRaw "/doctor/alerts" $doctorToken
        Write-Host "[DOCTOR_ALERTS] status=$($doctorAlerts.status)"
        Write-Host "[DOCTOR_ALERTS-BODY] $($doctorAlerts.body)"
      } else {
        Write-Host "[WARN] doctor token missing, CASE-2 partial"
      }
    }
  }

  Write-Title "CASE-3 doctor group management"
  if ($doctorToken -and $patientUserId) {
    $groupName = "api_group_$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
    $groupCreate = Invoke-ApiPost "/doctor/groups" @{ groupName = $groupName; description = "api acceptance" } $doctorToken
    Write-Host "[GROUP_CREATE]" ($groupCreate | ConvertTo-Json -Depth 6)

    $groupList = Invoke-RestMethod -Method Get -Uri ("$BaseUrl/doctor/groups") -Headers @{ "Authorization" = "Bearer $doctorToken" }
    $createdGroup = $null
    if ($groupList -and $groupList.data) {
      $createdGroup = $groupList.data | Where-Object { $_.groupName -eq $groupName } | Select-Object -First 1
    }

    if ($createdGroup) {
      $addRes = Invoke-ApiPost "/doctor/groups/$($createdGroup.id)/patients" @{ patientUserId = [int64]$patientUserId } $doctorToken
      Write-Host "[GROUP_ADD_PATIENT]" ($addRes | ConvertTo-Json -Depth 6)
      try {
        $groupPatients = Invoke-RestMethod -Method Get -Uri ("$BaseUrl/doctor/groups/$($createdGroup.id)/patients") -Headers @{ "Authorization" = "Bearer $doctorToken" }
        Write-Host "[GROUP_PATIENTS] status=200"
        Write-Host "[GROUP_PATIENTS-BODY]" ($groupPatients | ConvertTo-Json -Depth 6)
      } catch {
        Write-Host "[WARN] group patients query failed, CASE-3 partial:" $_.Exception.Message
      }
    } else {
      Write-Host "[WARN] group create/list mismatch, CASE-3 partial"
    }
  } else {
    Write-Host "[WARN] doctorToken or patientUserId missing, skip CASE-3"
  }

  Write-Title "CASE-4 admin role permission management"
  if ($adminToken) {
    $roleList = Invoke-RestMethod -Method Get -Uri ("$BaseUrl/admin/roles") -Headers @{ "Authorization" = "Bearer $adminToken" }
    Write-Host "[ROLE_LIST]" ($roleList | ConvertTo-Json -Depth 6)
    if ($roleList -and $roleList.data -and $roleList.data.Count -gt 0) {
      $firstRole = $roleList.data[0]
      $roleUpdate = Invoke-RestMethod -Method Put -Uri ("$BaseUrl/admin/roles") -Headers @{ "Authorization" = "Bearer $adminToken"; "Content-Type" = "application/json" } -Body (@{ id = $firstRole.id; permission = $firstRole.permission } | ConvertTo-Json)
      Write-Host "[ROLE_UPDATE]" ($roleUpdate | ConvertTo-Json -Depth 6)
    } else {
      Write-Host "[WARN] empty role list, CASE-4 partial"
    }
  } else {
    Write-Host "[WARN] admin token missing, skip CASE-4"
  }

  Write-Title "CASE-5 same account relogin should invalidate old token"
  $loginA = Invoke-ApiPost "/auth/login" @{ username = $AdminUser; password = $AdminPass }
  $tokenA = Get-Token $loginA
  $loginB = Invoke-ApiPost "/auth/login" @{ username = $AdminUser; password = $AdminPass }
  $tokenB = Get-Token $loginB

  if ($tokenA -and $tokenB) {
    $oldRes = Invoke-ApiGetRaw "/admin/user" $tokenA
    $newRes = Invoke-ApiGetRaw "/admin/user" $tokenB
    Write-Host "[CASE-5] oldTokenHttp=$($oldRes.status), newTokenHttp=$($newRes.status)"
    Write-Host "[CASE-5-OLD-BODY] $($oldRes.body)"
    Write-Host "[CASE-5-NEW-BODY] $($newRes.body)"
  } else {
    Write-Host "[WARN] tokens missing, skip CASE-5"
  }

  Write-Title "CASE-6 different accounts should not affect each other"
  $doctorLogin2 = Invoke-ApiPost "/auth/login" @{ username = $DoctorUser; password = $DoctorPass }
  $doctorToken2 = Get-Token $doctorLogin2

  if ($tokenB -and $doctorToken2) {
    $adminRes = Invoke-ApiGetRaw "/admin/user" $tokenB
    $doctorRes = Invoke-ApiGetRaw "/doctor/alerts" $doctorToken2
    Write-Host "[CASE-6] adminHttp=$($adminRes.status), doctorHttp=$($doctorRes.status)"
    Write-Host "[CASE-6-ADMIN-BODY] $($adminRes.body)"
    Write-Host "[CASE-6-DOCTOR-BODY] $($doctorRes.body)"
  } else {
    Write-Host "[WARN] tokens missing, skip CASE-6"
  }

  Write-Title "CASE-7 admin alert rule management"
   $adminLogin2 = Invoke-ApiPost "/auth/login" @{ username = $AdminUser; password = $AdminPass }
   $adminToken2 = Get-Token $adminLogin2
  if ($adminToken2) {
    $ruleList = Invoke-RestMethod -Method Get -Uri ("$BaseUrl/admin/config/alert-rules") -Headers @{ "Authorization" = "Bearer $adminToken2" }
    Write-Host "[RULE_LIST]" ($ruleList | ConvertTo-Json -Depth 6)
    if ($ruleList -and $ruleList.data -and $ruleList.data.Count -gt 0) {
      $bpRule = $ruleList.data | Select-Object -First 1
      if ($bpRule) {
        $ruleUpdateBody = @{ id = $bpRule.id; indicatorType = $bpRule.indicatorType; highRule = $bpRule.highRule; mediumRule = $bpRule.mediumRule; enabled = $bpRule.enabled } | ConvertTo-Json
        $ruleUpdate = Invoke-RestMethod -Method Put -Uri ("$BaseUrl/admin/config/alert-rules") -Headers @{ "Authorization" = "Bearer $adminToken2"; "Content-Type" = "application/json" } -Body $ruleUpdateBody
        Write-Host "[RULE_UPDATE]" ($ruleUpdate | ConvertTo-Json -Depth 6)
      } else {
        Write-Host "[WARN] blood pressure rule not found, CASE-7 partial"
      }
    } else {
      Write-Host "[WARN] empty alert rules, CASE-7 partial"
    }
  } else {
    Write-Host "[WARN] admin token missing, skip CASE-7"
  }

  Write-Title "CASE-8 doctor patient insight"
  if ($doctorToken2 -and $patientUserId) {
    $insightPath = "/doctor/patients/$patientUserId/insight?indicatorType=%E8%A1%80%E5%8E%8B&timeRange=month"
    $insight = Invoke-ApiGetRaw $insightPath $doctorToken2
    Write-Host "[PATIENT_INSIGHT] status=$($insight.status)"
    Write-Host "[PATIENT_INSIGHT-BODY] $($insight.body)"
  } else {
    Write-Host "[WARN] doctor token or patient id missing, skip CASE-8"
  }
} else {
  Write-Host "[INFO] assert-only mode enabled, skip CASE-1..CASE-8"
}

Write-Title "CASE-9 error-code assertions (400/401/403/404/409)"

# 400: bad request (bean validation)
$badReq = Invoke-ApiPostRaw "/auth/register" @{ username = ""; password = "123456"; phone = "13800009999"; name = "bad" }
$badReqCode = Get-BodyCode $badReq.body
Assert-Equal "400 badRequest body.code" "400" $badReqCode

# 401: unauthorized (no token)
$unauth = Invoke-ApiGetRaw "/admin/user"
$unauthCode = Get-BodyCode $unauth.body
Assert-Equal "401 unauthorized body.code" "401" $unauthCode

# 403: forbidden (doctor token access admin endpoint)
$doctorTokenForAssert = $doctorToken2
if (-not $doctorTokenForAssert) {
  $doctorLoginForAssert = Invoke-ApiPost "/auth/login" @{ username = $DoctorUser; password = $DoctorPass }
  $doctorTokenForAssert = Get-Token $doctorLoginForAssert
}
if ($doctorTokenForAssert) {
  $forbidden = Invoke-ApiGetRaw "/admin/user" $doctorTokenForAssert
  $forbiddenCode = Get-BodyCode $forbidden.body
  Assert-Equal "403 forbidden body.code" "403" $forbiddenCode
} else {
  Write-Host "[WARN] missing doctor token, skip 403 assertion"
}

# 404: not found (doctor queries non-existent patient insight)
if ($doctorTokenForAssert) {
  $notFound = Invoke-ApiGetRaw "/doctor/patients/99999999/insight?indicatorType=%E8%A1%80%E5%8E%8B&timeRange=month" $doctorTokenForAssert
  $notFoundCode = Get-BodyCode $notFound.body
  Assert-Equal "404 notFound body.code" "404" $notFoundCode
} else {
  Write-Host "[WARN] missing doctor token, skip 404 assertion"
}

# 409: conflict (register duplicate username)
$dupSeed = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$conflictUser = "dup_case_$dupSeed"
[void](Invoke-ApiPostRaw "/auth/register" @{ username = $conflictUser; password = "123456"; phone = "139$($dupSeed.ToString().PadLeft(8, '0').Substring(0,8))"; name = "dup user" })
$conflict = Invoke-ApiPostRaw "/auth/register" @{ username = $conflictUser; password = "123456"; phone = "139$($dupSeed.ToString().PadLeft(8, '0').Substring(0,8))"; name = "dup user" }
$conflictCode = Get-BodyCode $conflict.body
Assert-Equal "409 conflict body.code" "409" $conflictCode

if ($script:AssertFailed -gt 0) {
  Write-Host "[ASSERT-SUMMARY] failed=$($script:AssertFailed)"
  exit 1
}

Write-Host "[ASSERT-SUMMARY] all passed"
