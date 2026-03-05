#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080/api}"
PATIENT_USER="${PATIENT_USER:-patient_test_01}"
PATIENT_PASS="${PATIENT_PASS:-123456}"
PATIENT_PHONE="${PATIENT_PHONE:-13800009999}"
PATIENT_NAME="${PATIENT_NAME:-测试患者}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-123456}"

echo "[INFO] BASE_URL=${BASE_URL}"

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

extract_token() {
  python - <<'PY'
import json,sys
raw=sys.stdin.read()
try:
    data=json.loads(raw)
    print(data.get("data",{}).get("token", ""))
except Exception:
    print("")
PY
}

echo "\n[CASE-1] 患者注册 -> 登录 -> 上报血压 -> 查看数据"
REGISTER_RESP=$(post_json "/auth/register" "{\"username\":\"${PATIENT_USER}\",\"password\":\"${PATIENT_PASS}\",\"phone\":\"${PATIENT_PHONE}\",\"name\":\"${PATIENT_NAME}\"}")
echo "[REGISTER] ${REGISTER_RESP}"

LOGIN_RESP=$(post_json "/auth/login" "{\"username\":\"${PATIENT_USER}\",\"password\":\"${PATIENT_PASS}\"}")
echo "[LOGIN] ${LOGIN_RESP}"
PATIENT_TOKEN=$(echo "$LOGIN_RESP" | extract_token)
if [[ -z "$PATIENT_TOKEN" ]]; then
  echo "[WARN] 患者登录未获取到 token，请检查接口返回。"
else
  REPORT_RESP=$(post_json "/patient/data" '{"indicatorType":"血压","value":"135/88","remark":"接口测试上报"}' "$PATIENT_TOKEN")
  echo "[REPORT] ${REPORT_RESP}"
  LIST_RESP=$(curl -sS -H "Authorization: Bearer ${PATIENT_TOKEN}" "${BASE_URL}/patient/data?indicator_type=血压&timeRange=week")
  echo "[LIST] ${LIST_RESP}"
fi

echo "\n[CASE-2] 配置预警规则 -> 连续3天高血糖 -> 医生预警（若系统已实现预警模块）"
ADMIN_LOGIN=$(post_json "/auth/login" "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")
ADMIN_TOKEN=$(echo "$ADMIN_LOGIN" | extract_token)
if [[ -z "$ADMIN_TOKEN" ]]; then
  echo "[WARN] 管理员登录失败，跳过 CASE-2。"
  exit 0
fi

RULE_CREATE=$(post_json "/admin/rule" '{"rule_name":"连续3天高血糖","indicator_type":"血糖","threshold":"7.0","trigger_condition":3,"risk_level":"red"}' "$ADMIN_TOKEN" || true)
echo "[RULE_CREATE] ${RULE_CREATE}"

if [[ -n "$PATIENT_TOKEN" ]]; then
  for i in 1 2 3; do
    RESP=$(post_json "/patient/data" "{\"indicatorType\":\"血糖\",\"value\":\"8.${i}\",\"remark\":\"连续高血糖测试 day-${i}\"}" "$PATIENT_TOKEN" || true)
    echo "[GLUCOSE_DAY_${i}] ${RESP}"
  done
fi

echo "[INFO] 若系统已实现医生预警接口，可继续调用: GET /doctor/warning 验证预警生成。"
