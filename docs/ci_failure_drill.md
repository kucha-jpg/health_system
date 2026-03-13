# CI 可控故障演练脚本（quality-gate）

## 1. 目标
- 用一次可回滚、可复现、低风险的方式演练：
  1. 如何制造门禁失败
  2. 如何定位失败步骤
  3. 如何回滚修复并恢复通过

## 2. 演练原则
- 仅在临时分支演练，不在 `main` 直接注入故障。
- 故障注入应可 1 条提交回滚。
- 演练结束后删除临时分支。

## 3. 演练前检查
```powershell
git status -sb
```
确保工作区干净。

## 4. 演练流程

### Step A：创建临时分支
```powershell
git switch -c drill/quality-gate-failure
```

### Step B：注入可控故障（推荐：shellcheck 步骤）
在 `.github/workflows/quality-gate.yml` 的 `Shell script lint gate` 中，临时改为包含不存在的脚本：
```yaml
shellcheck -S error scripts/api_test.sh scripts/api_assert.sh scripts/not_exists.sh
```

提交并推送临时分支：
```powershell
git add .github/workflows/quality-gate.yml
git commit -m "drill: inject quality-gate shellcheck failure"
git push -u origin drill/quality-gate-failure
```

### Step C：触发并观察失败
```powershell
& "C:\Program Files\GitHub CLI\gh.exe" workflow run quality-gate.yml --ref drill/quality-gate-failure -R "kucha-jpg/health_system"
& "C:\Program Files\GitHub CLI\gh.exe" run list -R "kucha-jpg/health_system" --workflow quality-gate.yml --limit 5
& "C:\Program Files\GitHub CLI\gh.exe" run view <run_id> -R "kucha-jpg/health_system" --json jobs
& "C:\Program Files\GitHub CLI\gh.exe" run view <run_id> -R "kucha-jpg/health_system" --log-failed
```
预期：失败步骤为 `Shell script lint gate`。

### Step D：回滚修复
把 `scripts/not_exists.sh` 从 workflow 移除（恢复原命令），然后：
```powershell
git add .github/workflows/quality-gate.yml
git commit -m "drill: rollback injected shellcheck failure"
git push
```

### Step E：复验恢复
```powershell
& "C:\Program Files\GitHub CLI\gh.exe" workflow run quality-gate.yml --ref drill/quality-gate-failure -R "kucha-jpg/health_system"
```
轮询结果直到 `conclusion=success`。

### Step F：清理演练分支
```powershell
git switch main
git branch -D drill/quality-gate-failure
git push origin --delete drill/quality-gate-failure
```

## 5. 通过标准
- 至少 1 次可控失败、1 次修复后成功。
- 能在 10 分钟内定位到失败步骤并给出修复点。
- 演练分支已清理，`main` 无污染。

## 6. 常见偏差
- 在 `main` 演练：应立即停止并改到临时分支。
- 失败后直接改业务代码：优先定位当前失败步骤，不跨层排查。
- 忽略 `log-failed`：会显著拉长定位时间。
