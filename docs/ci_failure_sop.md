# CI 门禁失败处理 SOP（quality-gate）

## 1. 适用范围
- 工作流：`quality-gate`
- 场景：`push` / `pull_request` / 手动触发失败

## 2. 快速原则
- 先定位失败步骤，再看对应日志，不要先全量翻日志。
- 先复用现有修复路径（脚本/测试/迁移），避免重复试错。
- 每次修复后必须重新触发 `quality-gate` 验证。

## 3. 标准处理流程

### Step 1：确认失败 run 与失败步骤
```powershell
& "C:\Program Files\GitHub CLI\gh.exe" run list -R "kucha-jpg/health_system" --workflow quality-gate.yml --limit 5
& "C:\Program Files\GitHub CLI\gh.exe" run view <run_id> -R "kucha-jpg/health_system" --json jobs
```

### Step 2：提取失败日志
```powershell
& "C:\Program Files\GitHub CLI\gh.exe" run view <run_id> -R "kucha-jpg/health_system" --log-failed
```

若步骤中出现 `Upload surefire reports on failure`，优先下载 `surefire-reports` 工件定位具体测试失败。

### Step 3：按失败类型分流
- `Backend critical integration tests` / `Backend full test suite` 失败：
  - 先本地用 Docker Maven 复现单测：
```powershell
$p=(Get-Location).Path

docker run --rm -v "${p}/backend:/workspace" -w /workspace maven:3.9-eclipse-temurin-17 mvn -q "-Dtest=<TestClass>" test
```
  - 若是迁移类失败，优先检查：
    - Flyway baseline 配置
    - 测试库权限（是否依赖 `CREATE DATABASE`）
    - `V11/V12` 目标断言是否只覆盖回归目标
- `Shell script lint gate` 失败：
  - 本地执行：
```bash
shellcheck -S error scripts/api_test.sh scripts/api_assert.sh
```
- `API error-code assertions gate` 失败：
  - 先确认后端启动后再跑：
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\api_assert.ps1 -BaseUrl "http://127.0.0.1:9090/api"
```

### Step 4：提交修复并复验
```powershell
git add <files>
git commit -m "<type>: <message>"
git push origin main
& "C:\Program Files\GitHub CLI\gh.exe" workflow run quality-gate.yml --ref main -R "kucha-jpg/health_system"
```

### Step 5：等待并确认结果
```powershell
& "C:\Program Files\GitHub CLI\gh.exe" run view <new_run_id> -R "kucha-jpg/health_system" --json status,conclusion,jobs,url
```
- `conclusion=success`：关闭问题。
- `conclusion=failure`：回到 Step 2，按最新失败步骤继续处理。

## 4. 本项目已验证的高频故障点
- Flyway 集成测试：
  - 无 Docker 环境时自动跳过（`@Testcontainers(disabledWithoutDocker = true)`）。
  - 非空 schema 且无历史表：测试侧使用 `baselineOnMigrate=true` + `baselineVersion=10`。
  - 避免依赖测试账号建库权限（不再要求 `CREATE DATABASE`）。
- 安全错误码集成测试：
  - 断言按当前契约：HTTP 200 + 业务码 `401/403`。
- CI 门禁顺序：
  - 先关键测试（`SecurityErrorCodeIntegrationTest`、`FlywayMigrationIntegrationTest`）再全量测试。

## 5. 退出条件
- 最新 `quality-gate` 为 `success`。
- 对应修复已合并到 `main`。
- 必要时已在 `docs/testing_cases.md` 或 `docs/refactor_completion_report.md` 追加经验。

## 6. 配套演练
- 可控故障演练脚本：`docs/ci_failure_drill.md`