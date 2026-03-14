# Defense Evidence One-Pager

Date: 2026-03-14
Project: health_system

## 1. Final compliance statement

The system now provides an end-to-end evidence chain across:

- implementation (backend + frontend)
- acceptance scripts (PowerShell + Bash)
- CI gate (GitHub Actions)
- operation docs (README + CI SOP + CI drill)

Status: report-aligned with known environment note below.

Environment note:

- Current local Windows host does not provide `/bin/bash`, so `scripts/api_test.sh` cannot be executed locally here.
- The CI runner is `ubuntu-latest`, where `api_test.sh` is executed in the quality gate.

## 2. Major gaps that were closed

### Gap A: Script coverage mismatch (report claimed scenario 10/11, script had only 1-9)

Fix completed:

- Added CASE-10 and CASE-11 to PowerShell script.
- Added CASE-10 and CASE-11 to Bash script.

Evidence paths:

- `scripts/api_test.ps1`
- `scripts/api_test.sh`
- `docs/testing_cases.md`
- `docs/report_requirements_matrix.md`

Validation:

- Local run passed for PowerShell script:
  - `powershell -ExecutionPolicy Bypass -File .\scripts\api_test.ps1 -BaseUrl "http://127.0.0.1:9090/api"`
- CASE-10 and CASE-11 assertions reported OK in output.

### Gap B: CI gate only checked error-code script

Fix completed:

- Upgraded quality gate to run full API regression first, then error-code assertion.

Evidence paths:

- `.github/workflows/quality-gate.yml`
  - executes `./scripts/api_test.sh`
  - executes `./scripts/api_assert.sh`

### Gap C: Docker docs inconsistent with real startup config

Fix completed:

- README now reflects MySQL host port `3307`.
- README now reflects Flyway-based DB initialization and removal of automatic `health_system.sql` mount.

Evidence paths:

- `docker-compose.yml`
- `README.md`

### Gap D: CI operation docs not aligned with new gate order

Fix completed:

- Updated SOP and drill docs to follow current gate order and troubleshooting path.

Evidence paths:

- `docs/ci_failure_sop.md`
- `docs/ci_failure_drill.md`

### Gap E: Report wording ambiguity on indicator-type management

Fix completed:

- Added explicit statement in requirements matrix:
  - current model is fixed indicator enum + validation
  - can be extended to admin-configurable model later

Evidence path:

- `docs/report_requirements_matrix.md`

## 3. Scenario-to-evidence map (defense quick view)

- Scenario 1-8:
  - implemented in API scripts and role modules
  - script evidence: `scripts/api_test.ps1`, `scripts/api_test.sh`
- Scenario 9 (400/401/403/404/409):
  - assertion evidence: `scripts/api_assert.ps1`, `scripts/api_assert.sh`
- Scenario 10 (risk filter/sort):
  - assertion evidence in both scripts
- Scenario 11 (team isolation + forbidden handle=403):
  - assertion evidence in both scripts

## 4. Recommended defense wording

Suggested sentence:

- "The project has completed a closed-loop verification chain from code implementation to CI automation. Acceptance scenarios 1-11 are covered by regression scripts, and error-code contracts (400/401/403/404/409) are enforced by dedicated gate scripts."

## 5. Fast re-check commands

PowerShell local (Windows):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\api_test.ps1 -BaseUrl "http://127.0.0.1:9090/api"
powershell -ExecutionPolicy Bypass -File .\scripts\api_assert.ps1 -BaseUrl "http://127.0.0.1:9090/api"
```

Bash local/CI (Linux or WSL):

```bash
BASE_URL=http://127.0.0.1:9090/api ./scripts/api_test.sh
BASE_URL=http://127.0.0.1:9090/api ./scripts/api_assert.sh
```

CI gate file:

- `.github/workflows/quality-gate.yml`

## 6. Current conclusion

As of 2026-03-14, the repository is report-ready with traceable evidence.
Remaining risk is operational only (local Bash runtime not available on this Windows host), not a code-function gap.
