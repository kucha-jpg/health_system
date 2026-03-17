#!/usr/bin/env bash
set -euo pipefail

ASSERT_FAILED=0
ASSERT_ONLY="${ASSERT_ONLY:-0}"
CLEANUP="${CLEANUP:-0}"

BASE_URL="${BASE_URL:-http://127.0.0.1:9090/api}"
PATIENT_USER="${PATIENT_USER:-patient_test_01}"
PATIENT_PASS="${PATIENT_PASS:-123456}"
PATIENT_PHONE="${PATIENT_PHONE:-13800009999}"
PATIENT_NAME="${PATIENT_NAME:-测试患者}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-123456}"
DOCTOR_USER="${DOCTOR_USER:-doctor_demo_01}"
DOCTOR_PASS="${DOCTOR_PASS:-123456}"
RUN_TAG="${RUN_TAG:-$(date +%s)}"
BP_INDICATOR="血压"
BP_INDICATOR_ENCODED="%E8%A1%80%E5%8E%8B"

RUN_TAG_SAFE=$(printf "%s" "$RUN_TAG" | tr -cd '[:alnum:]' | tr '[:upper:]' '[:lower:]')
if [[ -z "$RUN_TAG_SAFE" ]]; then
  RUN_TAG_SAFE="run"
fi
RUN_TAG_SAFE="${RUN_TAG_SAFE:0:16}"
TAG_NUM=$(printf "%s" "$RUN_TAG_SAFE" | cksum | awk '{print $1}')
DOCTOR_SCOPE_USER="doctor_scope_b_${RUN_TAG_SAFE}"
PATIENT_SCOPE_USER="patient_scope_b_${RUN_TAG_SAFE}"
DOCTOR_SCOPE_PHONE=$(printf "138%08d" $((TAG_NUM % 100000000)))
PATIENT_SCOPE_PHONE=$(printf "139%08d" $(((TAG_NUM + 1) % 100000000)))

PYTHON_BIN="python3"
if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
  PYTHON_BIN="python"
fi

echo "[INFO] BASE_URL=${BASE_URL}"
echo "[INFO] ASSERT_ONLY=${ASSERT_ONLY}"
echo "[INFO] CLEANUP=${CLEANUP}"
echo "[INFO] RUN_TAG=${RUN_TAG_SAFE}"
RUN_MARK="rt_${RUN_TAG_SAFE}"

PATIENT_TOKEN=""
PATIENT_USER_ID=""
ADMIN_TOKEN=""
TOKEN_B=""
DOCTOR_TOKEN_2=""
DOCTOR_B_TOKEN=""
PATIENT_B_TOKEN=""

post_json() {
  local path="$1"
  local payload="$2"
  local token="${3:-}"
  if [[ -n "$token" ]]; then
    curl -sS -H "Content-Type: application/json" -H "Authorization: Bearer ${token}" -X POST "${BASE_URL}${path}" -d "$payload"
  else
    curl -sS -H "Content-Type: application/json" -X POST "${BASE_URL}${path}" -d "$payload"
  fi
}

put_json_raw() {
  local path="$1"
  local payload="$2"
  local token="${3:-}"
  if [[ -n "$token" ]]; then
    curl -sS -X PUT -H "Content-Type: application/json" -H "Authorization: Bearer ${token}" "${BASE_URL}${path}" -d "$payload"
  else
    curl -sS -X PUT -H "Content-Type: application/json" "${BASE_URL}${path}" -d "$payload"
  fi
}

delete_raw() {
  local path="$1"
  local token="${2:-}"
  if [[ -n "$token" ]]; then
    curl -sS -X DELETE -H "Authorization: Bearer ${token}" "${BASE_URL}${path}"
  else
    curl -sS -X DELETE "${BASE_URL}${path}"
  fi
}

extract_code() {
  "$PYTHON_BIN" -c 'import json,sys
raw=sys.stdin.read()
try:
    data=json.loads(raw)
    print(data.get("code", ""))
except Exception:
    print("")'
}

assert_equal() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [[ "$expected" == "$actual" ]]; then
    echo "[ASSERT-OK] ${label} expected=${expected} actual=${actual}"
  else
    ASSERT_FAILED=$((ASSERT_FAILED + 1))
    echo "[ASSERT-FAIL] ${label} expected=${expected} actual=${actual}"
  fi
}

extract_token() {
  "$PYTHON_BIN" -c 'import json,sys
raw=sys.stdin.read()
try:
    data=json.loads(raw)
    print(data.get("data",{}).get("token", ""))
except Exception:
    print("")'
}

extract_user_id() {
  "$PYTHON_BIN" -c 'import json,sys
raw=sys.stdin.read()
try:
    data=json.loads(raw)
    print(data.get("data",{}).get("userInfo",{}).get("id", ""))
except Exception:
    print("")'
}

url_encode() {
  "$PYTHON_BIN" -c 'import sys, urllib.parse
s=sys.stdin.read().strip()
print(urllib.parse.quote(s, safe=""))'
}

ensure_admin_managed_user() {
  local admin_token="$1"
  local username="$2"
  local role_type="$3"
  local name="$4"
  local phone="$5"
  local password="${6:-123456}"
  [[ -z "$admin_token" ]] && return 0
  post_json "/admin/user" "{\"username\":\"${username}\",\"password\":\"${password}\",\"phone\":\"${phone}\",\"name\":\"${name}\",\"roleType\":\"${role_type}\",\"status\":1}" "$admin_token" >/dev/null || true
}

get_admin_user_id_by_username() {
  local admin_token="$1"
  local username="$2"
  [[ -z "$admin_token" ]] && { echo ""; return 0; }
  local raw
  raw=$(curl -sS -H "Authorization: Bearer ${admin_token}" "${BASE_URL}/admin/user?keyword=${username}")
  echo "$raw" | TARGET_USER="$username" "$PYTHON_BIN" -c 'import json,sys,os
u=os.environ.get("TARGET_USER","")
raw=sys.stdin.read()
try:
    arr=json.loads(raw).get("data",[])
    target=next((x for x in arr if x.get("username")==u), None)
    print(target.get("id") if target else "")
except Exception:
    print("")'
}

cleanup_patient_data_and_alerts() {
  local patient_token="$1"
  local doctor_token="$2"
  local run_mark="$3"
  local label="$4"
  [[ -z "$patient_token" ]] && return 0

  local data_raw
  data_raw=$(curl -sS -H "Authorization: Bearer ${patient_token}" "${BASE_URL}/patient/data?timeRange=month")
  local data_ids
  data_ids=$(echo "$data_raw" | RUN_MARK="$run_mark" "$PYTHON_BIN" -c 'import json,sys,os
mark=os.environ.get("RUN_MARK", "")
raw=sys.stdin.read()
try:
    arr=json.loads(raw).get("data",[]) or []
    ids=[str(x.get("id")) for x in arr if mark and mark in str(x.get("remark") or "") and x.get("id") is not None]
    print(" ".join(ids))
except Exception:
    print("")')

  if [[ -z "$data_ids" ]]; then
    echo "[CLEANUP] ${label}: no tagged data found"
    return 0
  fi

  for id in $data_ids; do
    delete_raw "/patient/data/${id}" "$patient_token" >/dev/null || true
  done
  echo "[CLEANUP] ${label}: deleted data ids=${data_ids}"

  if [[ -z "$doctor_token" ]]; then
    return 0
  fi

  local alerts_raw
  alerts_raw=$(curl -sS -H "Authorization: Bearer ${doctor_token}" "${BASE_URL}/doctor/alerts")
  local alert_ids
  alert_ids=$(echo "$alerts_raw" | DATA_IDS="$data_ids" "$PYTHON_BIN" -c 'import json,sys,os
raw=sys.stdin.read()
ids=set((os.environ.get("DATA_IDS","") or "").split())
try:
    arr=json.loads(raw).get("data",[]) or []
    out=[str(x.get("id")) for x in arr if str(x.get("healthDataId")) in ids and str(x.get("status") or "")=="OPEN" and x.get("id") is not None]
    print(" ".join(out))
except Exception:
    print("")')

  for aid in $alert_ids; do
    post_json "/doctor/alerts/${aid}/handle" '{"handleRemark":"cleanup by api_test.sh"}' "$doctor_token" >/dev/null || true
  done
  if [[ -n "$alert_ids" ]]; then
    echo "[CLEANUP] ${label}: handled alert ids=${alert_ids}"
  fi
}

if [[ "${ASSERT_ONLY}" != "1" ]]; then
  PRE_ADMIN_LOGIN=$(post_json "/auth/login" "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}" || true)
  PRE_ADMIN_TOKEN=$(echo "$PRE_ADMIN_LOGIN" | extract_token)
  if [[ -n "$PRE_ADMIN_TOKEN" ]]; then
    PRE_RULES=$(curl -sS -H "Authorization: Bearer ${PRE_ADMIN_TOKEN}" "${BASE_URL}/admin/config/alert-rules" || true)
    BP_FROM_RULE=$(echo "$PRE_RULES" | "$PYTHON_BIN" -c 'import json,sys
raw=sys.stdin.read()
try:
    arr=json.loads(raw).get("data",[]) or []
    r=next((x for x in arr if "/" in str(x.get("highRule") or "")), None)
    print(r.get("indicatorType") if r else "")
except Exception:
    print("")')
    if [[ -n "$BP_FROM_RULE" ]]; then
      BP_INDICATOR="$BP_FROM_RULE"
    fi
  fi
  BP_INDICATOR_ENCODED=$(printf "%s" "$BP_INDICATOR" | url_encode)

  echo "\n[CASE-1] 患者注册 -> 登录 -> 上报血压 -> 查看数据"
  REGISTER_RESP=$(post_json "/auth/register" "{\"username\":\"${PATIENT_USER}\",\"password\":\"${PATIENT_PASS}\",\"phone\":\"${PATIENT_PHONE}\",\"name\":\"${PATIENT_NAME}\"}")
  echo "[REGISTER] ${REGISTER_RESP}"

  LOGIN_RESP=$(post_json "/auth/login" "{\"username\":\"${PATIENT_USER}\",\"password\":\"${PATIENT_PASS}\"}")
  echo "[LOGIN] ${LOGIN_RESP}"
  PATIENT_TOKEN=$(echo "$LOGIN_RESP" | extract_token)
  PATIENT_USER_ID=$(echo "$LOGIN_RESP" | extract_user_id)
  if [[ -z "$PATIENT_TOKEN" ]]; then
    echo "[WARN] 患者登录未获取到 token，请检查接口返回。"
  else
    REPORT_RESP=$(post_json "/patient/data" "{\"indicatorType\":\"${BP_INDICATOR}\",\"value\":\"135/88\",\"remark\":\"api test ${RUN_MARK}\"}" "$PATIENT_TOKEN")
    echo "[REPORT] ${REPORT_RESP}"
    LIST_RESP=$(curl -sS -H "Authorization: Bearer ${PATIENT_TOKEN}" "${BASE_URL}/patient/data?indicator_type=${BP_INDICATOR_ENCODED}&timeRange=week")
    echo "[LIST] ${LIST_RESP}"
  fi

  echo "\n[CASE-2] 患者上报异常指标 -> 医生收到预警"
  ADMIN_LOGIN=$(post_json "/auth/login" "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")
  ADMIN_TOKEN=$(echo "$ADMIN_LOGIN" | extract_token)
  ensure_admin_managed_user "$ADMIN_TOKEN" "$DOCTOR_USER" "DOCTOR" "doctor demo" "13800006666"
  DOCTOR_LOGIN=$(post_json "/auth/login" "{\"username\":\"${DOCTOR_USER}\",\"password\":\"${DOCTOR_PASS}\"}")
  DOCTOR_TOKEN=$(echo "$DOCTOR_LOGIN" | extract_token)
  if [[ -z "$ADMIN_TOKEN" ]]; then
    echo "[WARN] 管理员登录失败，跳过 CASE-2。"
    exit 0
  fi

  if [[ -n "$PATIENT_TOKEN" ]]; then
    ALERT_REPORT=$(post_json "/patient/data" "{\"indicatorType\":\"${BP_INDICATOR}\",\"value\":\"190/120\",\"remark\":\"alert case ${RUN_MARK}\"}" "$PATIENT_TOKEN" || true)
    echo "[ALERT_REPORT] ${ALERT_REPORT}"
    if [[ -n "$DOCTOR_TOKEN" ]]; then
      DOCTOR_ALERTS=$(curl -sS -H "Authorization: Bearer ${DOCTOR_TOKEN}" "${BASE_URL}/doctor/alerts")
      echo "[DOCTOR_ALERTS] ${DOCTOR_ALERTS}"
    fi
  fi

  echo "\n[CASE-3] 医生群组管理"
  if [[ -n "$DOCTOR_TOKEN" ]]; then
    GROUP_NAME="api_group_$(date +%s)"
    GROUP_CREATE=$(post_json "/doctor/groups" "{\"groupName\":\"${GROUP_NAME}\",\"description\":\"api acceptance\"}" "$DOCTOR_TOKEN" || true)
    echo "[GROUP_CREATE] ${GROUP_CREATE}"
    GROUP_LIST=$(curl -sS -H "Authorization: Bearer ${DOCTOR_TOKEN}" "${BASE_URL}/doctor/groups")
    echo "[GROUP_LIST] ${GROUP_LIST}"
    GROUP_ID=$(echo "$GROUP_LIST" | GROUP_NAME="$GROUP_NAME" "$PYTHON_BIN" -c 'import json,sys,os
name=os.environ.get("GROUP_NAME", "")
raw=sys.stdin.read()
try:
    data=json.loads(raw).get("data", [])
    target=next((x for x in data if x.get("groupName") == name), None)
    print(target.get("id") if target else "")
except Exception:
    print("")')
    if [[ -n "$GROUP_ID" && -n "$PATIENT_USER_ID" ]]; then
      GROUP_ADD=$(post_json "/doctor/groups/${GROUP_ID}/patients" "{\"patientUserId\":${PATIENT_USER_ID}}" "$DOCTOR_TOKEN" || true)
      echo "[GROUP_ADD] ${GROUP_ADD}"
      GROUP_PATIENTS=$(curl -sS -H "Authorization: Bearer ${DOCTOR_TOKEN}" "${BASE_URL}/doctor/groups/${GROUP_ID}/patients")
      echo "[GROUP_PATIENTS] ${GROUP_PATIENTS}"
    else
      echo "[WARN] 缺少 GROUP_ID 或 PATIENT_USER_ID，CASE-3 部分跳过。"
    fi
  fi

  echo "\n[CASE-4] 管理员角色权限管理"
  ROLE_LIST=$(curl -sS -H "Authorization: Bearer ${ADMIN_TOKEN}" "${BASE_URL}/admin/roles")
  echo "[ROLE_LIST] ${ROLE_LIST}"
  ROLE_PAYLOAD=$(echo "$ROLE_LIST" | "$PYTHON_BIN" -c 'import json,sys
raw=sys.stdin.read()
try:
    arr=json.loads(raw).get("data", [])
    if not arr:
        print("")
    else:
        r=arr[0]
        print(json.dumps({"id": r.get("id"), "permission": r.get("permission")}, ensure_ascii=False))
except Exception:
    print("")')
  if [[ -n "$ROLE_PAYLOAD" ]]; then
    ROLE_UPDATE=$(curl -sS -H "Content-Type: application/json" -H "Authorization: Bearer ${ADMIN_TOKEN}" -X PUT "${BASE_URL}/admin/roles" -d "$ROLE_PAYLOAD")
    echo "[ROLE_UPDATE] ${ROLE_UPDATE}"
  fi

  echo "\n[CASE-5] 同账号重复登录：新登录应使旧 token 失效"
  LOGIN_A=$(post_json "/auth/login" "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")
  TOKEN_A=$(echo "$LOGIN_A" | extract_token)
  LOGIN_B=$(post_json "/auth/login" "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")
  TOKEN_B=$(echo "$LOGIN_B" | extract_token)

  if [[ -z "$TOKEN_A" || -z "$TOKEN_B" ]]; then
    echo "[WARN] 同账号重复登录未获取到 token，跳过 CASE-3。"
  else
    ADMIN_TOKEN="$TOKEN_B"
    OLD_HTTP=$(curl -sS -o /tmp/case3_old.json -w "%{http_code}" -H "Authorization: Bearer ${TOKEN_A}" "${BASE_URL}/admin/user")
    NEW_HTTP=$(curl -sS -o /tmp/case3_new.json -w "%{http_code}" -H "Authorization: Bearer ${TOKEN_B}" "${BASE_URL}/admin/user")
    echo "[CASE-5] oldTokenHttp=${OLD_HTTP}, newTokenHttp=${NEW_HTTP}"
    echo "[CASE-5-OLD-BODY] $(cat /tmp/case3_old.json)"
    echo "[CASE-5-NEW-BODY] $(cat /tmp/case3_new.json)"
  fi

  echo "\n[CASE-6] 不同账号登录互不干扰"
  DOCTOR_LOGIN_2=$(post_json "/auth/login" "{\"username\":\"${DOCTOR_USER}\",\"password\":\"${DOCTOR_PASS}\"}")
  DOCTOR_TOKEN_2=$(echo "$DOCTOR_LOGIN_2" | extract_token)

  if [[ -z "$TOKEN_B" || -z "$DOCTOR_TOKEN_2" ]]; then
    echo "[WARN] 账号 token 不完整，跳过 CASE-4。"
  else
    ADMIN_HTTP=$(curl -sS -o /tmp/case4_admin.json -w "%{http_code}" -H "Authorization: Bearer ${TOKEN_B}" "${BASE_URL}/admin/user")
    DOCTOR_HTTP=$(curl -sS -o /tmp/case4_doctor.json -w "%{http_code}" -H "Authorization: Bearer ${DOCTOR_TOKEN_2}" "${BASE_URL}/doctor/alerts")
    echo "[CASE-6] adminHttp=${ADMIN_HTTP}, doctorHttp=${DOCTOR_HTTP}"
    echo "[CASE-6-ADMIN-BODY] $(cat /tmp/case4_admin.json)"
    echo "[CASE-6-DOCTOR-BODY] $(cat /tmp/case4_doctor.json)"
  fi

  echo "\n[CASE-7] 预警规则管理"
  RULE_LIST=$(curl -sS -H "Authorization: Bearer ${ADMIN_TOKEN}" "${BASE_URL}/admin/config/alert-rules")
  echo "[RULE_LIST] ${RULE_LIST}"

  echo "\n[CASE-8] 医生患者洞察"
  if [[ -n "$DOCTOR_TOKEN_2" && -n "$PATIENT_USER_ID" ]]; then
    INSIGHT=$(curl -sS -H "Authorization: Bearer ${DOCTOR_TOKEN_2}" "${BASE_URL}/doctor/patients/${PATIENT_USER_ID}/insight?indicatorType=${BP_INDICATOR_ENCODED}&timeRange=month")
    echo "[PATIENT_INSIGHT] ${INSIGHT}"
  else
    echo "[WARN] 缺少 DOCTOR_TOKEN_2 或 PATIENT_USER_ID，CASE-8 部分跳过。"
  fi

  echo "\n[CASE-10] 医生按风险筛选与排序"
  if [[ -n "$DOCTOR_TOKEN_2" ]]; then
    CASE10_RAW=$(curl -sS -H "Authorization: Bearer ${DOCTOR_TOKEN_2}" "${BASE_URL}/doctor/alerts?riskLevel=HIGH&minRiskScore=80&sortBy=risk_desc")
    echo "[CASE-10-BODY] ${CASE10_RAW}"
    CASE10_OK=$(echo "$CASE10_RAW" | "$PYTHON_BIN" -c 'import json,sys
raw=sys.stdin.read()
try:
    obj=json.loads(raw)
    if obj.get("code") != 200:
        print("False")
        raise SystemExit
    arr=obj.get("data",[]) or []
    prev=10**9
    ok=True
    for it in arr:
        lv=(it.get("riskLevel") or "")
        sc=int(it.get("riskScore") or 0)
        if lv != "HIGH" or sc < 80 or sc > prev:
            ok=False
            break
        prev=sc
    print("True" if ok else "False")
except Exception:
    print("False")')
    assert_equal "CASE-10 filter-sort pass" "True" "$CASE10_OK"
  else
    echo "[WARN] 缺少 DOCTOR_TOKEN_2，跳过 CASE-10"
  fi

  echo "\n[CASE-11] 医生团队隔离与越权处理"
  if [[ -n "$ADMIN_TOKEN" && -n "$DOCTOR_TOKEN_2" ]]; then
    DOCTOR_B_USER="$DOCTOR_SCOPE_USER"
    PATIENT_B_USER="$PATIENT_SCOPE_USER"
    ensure_admin_managed_user "$ADMIN_TOKEN" "$DOCTOR_B_USER" "DOCTOR" "doctor scope b" "$DOCTOR_SCOPE_PHONE"
    ensure_admin_managed_user "$ADMIN_TOKEN" "$PATIENT_B_USER" "PATIENT" "patient scope b" "$PATIENT_SCOPE_PHONE"

    DOCTOR_B_ID=$(get_admin_user_id_by_username "$ADMIN_TOKEN" "$DOCTOR_B_USER")
    PATIENT_B_ID=$(get_admin_user_id_by_username "$ADMIN_TOKEN" "$PATIENT_B_USER")

    DOCTOR_B_LOGIN=$(post_json "/auth/login" "{\"username\":\"${DOCTOR_B_USER}\",\"password\":\"123456\"}")
    DOCTOR_B_TOKEN=$(echo "$DOCTOR_B_LOGIN" | extract_token)
    PATIENT_B_LOGIN=$(post_json "/auth/login" "{\"username\":\"${PATIENT_B_USER}\",\"password\":\"123456\"}")
    PATIENT_B_TOKEN=$(echo "$PATIENT_B_LOGIN" | extract_token)

    if [[ -n "$DOCTOR_B_ID" && -n "$PATIENT_B_ID" && -n "$DOCTOR_B_TOKEN" && -n "$PATIENT_B_TOKEN" ]]; then
      GROUP_NAME_B="scope_b_group_$(date +%s)"
      post_json "/doctor/groups" "{\"groupName\":\"${GROUP_NAME_B}\",\"description\":\"scope b\"}" "$DOCTOR_B_TOKEN" >/dev/null || true
      GROUP_LIST_B=$(curl -sS -H "Authorization: Bearer ${DOCTOR_B_TOKEN}" "${BASE_URL}/doctor/groups")
      GROUP_B_ID=$(echo "$GROUP_LIST_B" | GROUP_NAME="$GROUP_NAME_B" "$PYTHON_BIN" -c 'import json,sys,os
name=os.environ.get("GROUP_NAME", "")
raw=sys.stdin.read()
try:
    arr=json.loads(raw).get("data",[])
    target=next((x for x in arr if x.get("groupName")==name), None)
    print(target.get("id") if target else "")
except Exception:
    print("")')

      if [[ -n "$GROUP_B_ID" ]]; then
        post_json "/doctor/groups/${GROUP_B_ID}/patients" "{\"patientUserId\":${PATIENT_B_ID}}" "$DOCTOR_B_TOKEN" >/dev/null || true
        post_json "/patient/data" "{\"indicatorType\":\"${BP_INDICATOR}\",\"value\":\"190/120\",\"remark\":\"scope case ${RUN_MARK}\"}" "$PATIENT_B_TOKEN" >/dev/null || true

        DOCTOR_B_ALERTS=$(curl -sS -H "Authorization: Bearer ${DOCTOR_B_TOKEN}" "${BASE_URL}/doctor/alerts")
        TARGET_ALERT_ID=$(echo "$DOCTOR_B_ALERTS" | TARGET_UID="$PATIENT_B_ID" "$PYTHON_BIN" -c 'import json,sys,os
uid=int(os.environ.get("TARGET_UID","0") or 0)
raw=sys.stdin.read()
try:
    arr=json.loads(raw).get("data",[])
    target=next((x for x in arr if int(x.get("userId") or 0)==uid), None)
    print(target.get("id") if target else "")
except Exception:
    print("")')

        DOCTOR_A_ALERTS=$(curl -sS -H "Authorization: Bearer ${DOCTOR_TOKEN_2}" "${BASE_URL}/doctor/alerts")
        CASE11_LIST_OK=$(echo "$DOCTOR_A_ALERTS" | TARGET_UID="$PATIENT_B_ID" "$PYTHON_BIN" -c 'import json,sys,os
uid=int(os.environ.get("TARGET_UID","0") or 0)
raw=sys.stdin.read()
try:
    arr=json.loads(raw).get("data",[])
    contains=any(int(x.get("userId") or 0)==uid for x in arr)
    print("False" if contains else "True")
except Exception:
    print("False")')
        assert_equal "CASE-11 list isolation" "True" "$CASE11_LIST_OK"

        if [[ -n "$TARGET_ALERT_ID" ]]; then
          FORBIDDEN_RAW=$(post_json "/doctor/alerts/${TARGET_ALERT_ID}/handle" '{"handleRemark":"out-of-scope test"}' "$DOCTOR_TOKEN_2")
          FORBIDDEN_CODE=$(echo "$FORBIDDEN_RAW" | extract_code)
          assert_equal "CASE-11 forbidden handle" "403" "$FORBIDDEN_CODE"
        else
          echo "[WARN] 未找到 scope-B 预警，跳过 CASE-11 越权处理断言"
        fi
      else
        echo "[WARN] 未找到 scope-B 群组，跳过 CASE-11"
      fi
    else
      echo "[WARN] scope-B 账号或 token 不完整，跳过 CASE-11"
    fi
  else
    echo "[WARN] 缺少 ADMIN_TOKEN 或 DOCTOR_TOKEN_2，跳过 CASE-11"
  fi

  echo "\n[CASE-12] 指标类型启停联动"
  if [[ -n "$ADMIN_TOKEN" ]]; then
    if [[ ${#RUN_TAG_SAFE} -gt 8 ]]; then
      TEMP_SUFFIX="${RUN_TAG_SAFE: -8}"
    else
      TEMP_SUFFIX="${RUN_TAG_SAFE}"
    fi
    TEMP_TYPE="c12_${TEMP_SUFFIX}"
    TEMP_CREATE=$(post_json "/admin/config/indicator-types" "{\"indicatorType\":\"${TEMP_TYPE}\",\"displayName\":\"${TEMP_TYPE}\",\"enabled\":1}" "$ADMIN_TOKEN")
    TEMP_CREATE_CODE=$(echo "$TEMP_CREATE" | extract_code)
    if [[ "$TEMP_CREATE_CODE" != "200" && "$TEMP_CREATE_CODE" != "409" ]]; then
      assert_equal "CASE-12 create temp indicator" "200" "$TEMP_CREATE_CODE"
    fi

    TYPES_ALL=$(curl -sS -H "Authorization: Bearer ${ADMIN_TOKEN}" "${BASE_URL}/admin/config/indicator-types?includeDisabled=true")
    TYPE_JSON=$(echo "$TYPES_ALL" | TEMP_TYPE="$TEMP_TYPE" "$PYTHON_BIN" -c 'import json,sys,os
raw=sys.stdin.read()
target=os.environ.get("TEMP_TYPE", "")
try:
    arr=json.loads(raw).get("data",[]) or []
    t=next((x for x in arr if str(x.get("indicatorType") or "")==target), None)
    if not t:
        print("")
    else:
        print(json.dumps({"id":t.get("id"),"indicatorType":t.get("indicatorType"),"displayName":t.get("displayName")}, ensure_ascii=False))
except Exception:
    print("")')

    if [[ -n "$TYPE_JSON" ]]; then
      TYPE_DISABLE=$(echo "$TYPE_JSON" | "$PYTHON_BIN" -c 'import json,sys
t=json.loads(sys.stdin.read())
t["enabled"]=0
print(json.dumps(t, ensure_ascii=False))')
      DISABLE_RAW=$(put_json_raw "/admin/config/indicator-types" "$TYPE_DISABLE" "$ADMIN_TOKEN")
      DISABLE_CODE=$(echo "$DISABLE_RAW" | extract_code)
      assert_equal "CASE-12 disable indicator" "200" "$DISABLE_CODE"

      if [[ -n "$PATIENT_TOKEN" ]]; then
        TYPE_FOR_REPORT=$(echo "$TYPE_JSON" | "$PYTHON_BIN" -c 'import json,sys
print(json.loads(sys.stdin.read()).get("indicatorType", ""))')
        BLOCKED_RAW=$(post_json "/patient/data" "{\"indicatorType\":\"${TYPE_FOR_REPORT}\",\"value\":\"123\",\"remark\":\"case12 disabled ${RUN_MARK}\"}" "$PATIENT_TOKEN")
        BLOCKED_CODE=$(echo "$BLOCKED_RAW" | extract_code)
        assert_equal "CASE-12 report blocked" "400" "$BLOCKED_CODE"
      else
        echo "[WARN] 缺少 PATIENT_TOKEN，CASE-12 部分跳过"
      fi

      TYPE_ENABLE=$(echo "$TYPE_JSON" | "$PYTHON_BIN" -c 'import json,sys
t=json.loads(sys.stdin.read())
t["enabled"]=1
print(json.dumps(t, ensure_ascii=False))')
      ENABLE_RAW=$(put_json_raw "/admin/config/indicator-types" "$TYPE_ENABLE" "$ADMIN_TOKEN")
      ENABLE_CODE=$(echo "$ENABLE_RAW" | extract_code)
      assert_equal "CASE-12 restore indicator" "200" "$ENABLE_CODE"

      if [[ -n "$PATIENT_TOKEN" ]]; then
        RESTORE_RAW=$(post_json "/patient/data" "{\"indicatorType\":\"${TYPE_FOR_REPORT}\",\"value\":\"123\",\"remark\":\"case12 enabled ${RUN_MARK}\"}" "$PATIENT_TOKEN")
        RESTORE_CODE=$(echo "$RESTORE_RAW" | extract_code)
        assert_equal "CASE-12 report allowed after restore" "200" "$RESTORE_CODE"
      fi
    else
      echo "[WARN] 未找到临时指标，跳过 CASE-12"
    fi
  else
    echo "[WARN] 缺少 ADMIN_TOKEN，跳过 CASE-12"
  fi

  echo "\n[CASE-13] 血压连续3天主动预警"
  if [[ -n "$PATIENT_TOKEN" && -n "$DOCTOR_TOKEN_2" && -n "$ADMIN_TOKEN" ]]; then
    RULES13=$(curl -sS -H "Authorization: Bearer ${ADMIN_TOKEN}" "${BASE_URL}/admin/config/alert-rules")
    BP_INDICATOR=$(echo "$RULES13" | "$PYTHON_BIN" -c 'import json,sys
raw=sys.stdin.read()
try:
    arr=json.loads(raw).get("data",[]) or []
    r=next((x for x in arr if "/" in str(x.get("highRule") or "")), None)
    print(r.get("indicatorType") if r else "")
except Exception:
    print("")')
    if [[ -z "$BP_INDICATOR" ]]; then
      echo "[WARN] 未找到血压指标类型，跳过 CASE-13"
    else
    now_ts=$(date +%s)
    ts1=$((now_ts - 172800))
    ts2=$((now_ts - 86400))
    ts3=$now_ts
    rt1=$(date -d "@${ts1}" +"%Y-%m-%dT%H:%M:%S")
    rt2=$(date -d "@${ts2}" +"%Y-%m-%dT%H:%M:%S")
    rt3=$(date -d "@${ts3}" +"%Y-%m-%dT%H:%M:%S")

    R1=$(post_json "/patient/data" "{\"indicatorType\":\"${BP_INDICATOR}\",\"value\":\"150/95\",\"reportTime\":\"${rt1}\",\"remark\":\"case13 ${RUN_MARK}\"}" "$PATIENT_TOKEN")
    R2=$(post_json "/patient/data" "{\"indicatorType\":\"${BP_INDICATOR}\",\"value\":\"150/95\",\"reportTime\":\"${rt2}\",\"remark\":\"case13 ${RUN_MARK}\"}" "$PATIENT_TOKEN")
    R3=$(post_json "/patient/data" "{\"indicatorType\":\"${BP_INDICATOR}\",\"value\":\"150/95\",\"reportTime\":\"${rt3}\",\"remark\":\"case13 ${RUN_MARK}\"}" "$PATIENT_TOKEN")
    assert_equal "CASE-13 report accepted #1" "200" "$(echo "$R1" | extract_code)"
    assert_equal "CASE-13 report accepted #2" "200" "$(echo "$R2" | extract_code)"
    assert_equal "CASE-13 report accepted #3" "200" "$(echo "$R3" | extract_code)"

    ALERTS13=$(curl -sS -H "Authorization: Bearer ${DOCTOR_TOKEN_2}" "${BASE_URL}/doctor/alerts")
    CASE13_OK=$(echo "$ALERTS13" | "$PYTHON_BIN" -c 'import json,sys
raw=sys.stdin.read()
try:
    arr=json.loads(raw).get("data",[]) or []
    ok=any((x.get("reasonCode") or "")=="BP_PERSISTENT_HIGH" for x in arr)
    print("True" if ok else "False")
except Exception:
    print("False")')
    assert_equal "CASE-13 persistent alert generated" "True" "$CASE13_OK"
    fi
  else
    echo "[WARN] 缺少 PATIENT_TOKEN / DOCTOR_TOKEN_2 / ADMIN_TOKEN，跳过 CASE-13"
  fi
else
  echo "[INFO] assert-only mode enabled, skip CASE-1..CASE-13"
fi

echo "\n[CASE-9] 错误码断言（400/401/403/404/409）"

# 400: bad request (bean validation)
BAD_REQ=$(post_json "/auth/register" '{"username":"","password":"123456","phone":"13800009999","name":"bad"}')
BAD_REQ_CODE=$(echo "$BAD_REQ" | extract_code)
assert_equal "400 badRequest body.code" "400" "$BAD_REQ_CODE"

# 401: unauthorized
UNAUTHORIZED=$(curl -sS "${BASE_URL}/admin/user")
UNAUTHORIZED_CODE=$(echo "$UNAUTHORIZED" | extract_code)
assert_equal "401 unauthorized body.code" "401" "$UNAUTHORIZED_CODE"

# 403: forbidden
DOCTOR_TOKEN_ASSERT="${DOCTOR_TOKEN_2}"
if [[ -z "${DOCTOR_TOKEN_ASSERT}" ]]; then
  DOCTOR_LOGIN_ASSERT=$(post_json "/auth/login" "{\"username\":\"${DOCTOR_USER}\",\"password\":\"${DOCTOR_PASS}\"}")
  DOCTOR_TOKEN_ASSERT=$(echo "$DOCTOR_LOGIN_ASSERT" | extract_token)
fi

if [[ -n "$DOCTOR_TOKEN_ASSERT" ]]; then
  FORBIDDEN=$(curl -sS -H "Authorization: Bearer ${DOCTOR_TOKEN_ASSERT}" "${BASE_URL}/admin/user")
  FORBIDDEN_CODE=$(echo "$FORBIDDEN" | extract_code)
  assert_equal "403 forbidden body.code" "403" "$FORBIDDEN_CODE"
else
  echo "[WARN] 缺少 DOCTOR_TOKEN_2，跳过 403 断言"
fi

# 404: not found (doctor queries non-existent patient insight)
if [[ -n "$DOCTOR_TOKEN_ASSERT" ]]; then
  NOT_FOUND=$(curl -sS -H "Authorization: Bearer ${DOCTOR_TOKEN_ASSERT}" "${BASE_URL}/doctor/patients/99999999/insight?indicatorType=${BP_INDICATOR_ENCODED}&timeRange=month")
  NOT_FOUND_CODE=$(echo "$NOT_FOUND" | extract_code)
  assert_equal "404 notFound body.code" "404" "$NOT_FOUND_CODE"
else
  echo "[WARN] 缺少 DOCTOR_TOKEN_2，跳过 404 断言"
fi

# 409: conflict
RND_SUFFIX=$(date +%s)
CONFLICT_USER="dup_case_${RND_SUFFIX}"
CONFLICT_PHONE="139$(printf "%08d" $((RND_SUFFIX % 100000000)))"
post_json "/auth/register" "{\"username\":\"${CONFLICT_USER}\",\"password\":\"123456\",\"phone\":\"${CONFLICT_PHONE}\",\"name\":\"dup user\"}" >/dev/null
CONFLICT=$(post_json "/auth/register" "{\"username\":\"${CONFLICT_USER}\",\"password\":\"123456\",\"phone\":\"${CONFLICT_PHONE}\",\"name\":\"dup user\"}")
CONFLICT_CODE=$(echo "$CONFLICT" | extract_code)
assert_equal "409 conflict body.code" "409" "$CONFLICT_CODE"

if [[ "${ASSERT_ONLY}" != "1" && "${CLEANUP}" == "1" ]]; then
  echo "\n[CLEANUP] marker=${RUN_MARK}"
  cleanup_patient_data_and_alerts "$PATIENT_TOKEN" "$DOCTOR_TOKEN_2" "$RUN_MARK" "patient-main"
  cleanup_patient_data_and_alerts "$PATIENT_B_TOKEN" "$DOCTOR_B_TOKEN" "$RUN_MARK" "patient-scope-b"
fi

if [[ "$ASSERT_FAILED" -gt 0 ]]; then
  echo "[ASSERT-SUMMARY] failed=${ASSERT_FAILED}"
  exit 1
fi

echo "[ASSERT-SUMMARY] all passed"
