# 核心功能测试用例与前端测试步骤

## 一、核心测试用例

### 场景1：患者注册→登录→上报血压→查看数据

- 前置条件：后端服务正常，数据库已初始化。
- 步骤：
  1. 调用 `POST /api/auth/register` 注册患者。
  2. 调用 `POST /api/auth/login` 获取 token。
  3. 调用 `POST /api/patient/data` 上报血压（如 `135/88`）。
  4. 调用 `GET /api/patient/data?indicator_type=血压&timeRange=week` 查看数据。
- 预期结果：
  - 返回 `code=200`。
  - 列表中存在刚上报的血压数据。

### 场景2：患者上报异常指标→医生收到预警

- 前置条件：医生账号可登录，系统已开启预警判定逻辑。
- 步骤：
  1. 患者调用 `POST /api/patient/data` 上报异常数据（例如高血压 `190/120`）。
  2. 医生调用 `GET /api/doctor/alerts` 查询预警。
- 预期结果：
  - 患者上报成功。
  - 医生预警列表可看到对应患者预警记录。

### 场景3：医生群组管理

- 前置条件：医生账号可登录。
- 步骤：
  1. 调用 `POST /api/doctor/groups` 创建群组。
  2. 调用 `POST /api/doctor/groups/{groupId}/patients` 添加患者。
  3. 调用 `GET /api/doctor/groups/{groupId}/patients` 查看群组患者。
- 预期结果：
  - 群组创建成功。
  - 患者可加入群组且列表可见。

### 场景4：管理员角色权限管理

- 前置条件：管理员账号可登录。
- 步骤：
  1. 调用 `GET /api/admin/roles` 查询角色与权限字符串。
  2. 选择一个角色调用 `PUT /api/admin/roles` 更新权限字符串。
  3. 重新调用 `GET /api/admin/roles` 核对更新结果。
- 预期结果：
  - 角色列表查询成功，返回 `code=200`。
  - 更新接口返回成功，且查询结果与更新内容一致。

### 场景5：同账号新登录踢掉旧登录

- 前置条件：管理员账号可登录。
- 步骤：
  1. 第一次调用 `POST /api/auth/login` 获取 tokenA。
  2. 再次调用 `POST /api/auth/login` 获取 tokenB。
  3. 使用 tokenA 调用 `GET /api/admin/user`。
  4. 使用 tokenB 调用 `GET /api/admin/user`。
- 预期结果：
  - tokenA 返回 `401`（旧登录失效）。
  - tokenB 返回 `200`（新登录有效）。

### 场景6：不同账号登录互不影响

- 前置条件：管理员账号与医生账号均可登录。
- 步骤：
  1. 管理员调用 `POST /api/auth/login` 获取 tokenAdmin。
  2. 医生调用 `POST /api/auth/login` 获取 tokenDoctor。
  3. 使用 tokenAdmin 调用 `GET /api/admin/user`。
  4. 使用 tokenDoctor 调用 `GET /api/doctor/alerts`。
- 预期结果：
  - 两次调用均返回 `200`。
  - 登录状态互不干扰。

### 场景7：管理员预警规则管理

- 前置条件：管理员账号可登录。
- 步骤：
  1. 调用 `GET /api/admin/config/alert-rules` 查询规则列表。
  2. 选择一个规则调用 `PUT /api/admin/config/alert-rules` 更新规则参数（例如保持同值更新）。
  3. 再次调用 `GET /api/admin/config/alert-rules` 核对更新后规则可读。
- 预期结果：
  - 规则列表查询成功，返回 `code=200`。
  - 规则更新成功，返回 `code=200`。
  - 更新后规则可正常查询。

### 场景8：医生查看群组患者洞察

- 前置条件：医生账号可登录，且目标患者在该医生群组内。
- 步骤：
  1. 调用 `GET /api/doctor/patients/{patientUserId}/insight?indicatorType=血压&timeRange=month`。
  2. 检查返回中是否包含患者档案、趋势数据、近期预警等字段。
- 预期结果：
  - 接口返回 `code=200`。
  - 返回结构包含洞察所需核心数据。

### 场景10：医生按风险分筛选预警

- 前置条件：系统存在不同风险分的 OPEN 预警数据。
- 步骤：
  1. 调用 `GET /api/doctor/alerts?riskLevel=HIGH&minRiskScore=80&sortBy=risk_desc`。
  2. 检查结果是否仅返回 `riskLevel=HIGH` 且 `riskScore>=80` 的预警。
  3. 验证返回记录按风险分降序排列。
- 预期结果：
  - 接口返回 `code=200`。
  - 结果满足过滤条件且排序正确。

### 场景11：医生仅可查看团队患者预警

- 前置条件：存在 A 医生团队与 B 医生团队，且两队各自有患者预警数据。
- 步骤：
  1. 使用 A 医生 token 调用 `GET /api/doctor/alerts`。
  2. 校验返回数据中的 `userId` 仅属于 A 医生可访问的群组患者。
  3. 尝试用 A 医生处理 B 团队患者的预警 `POST /api/doctor/alerts/{id}/handle`。
- 预期结果：
  - 列表接口返回 `code=200` 且不包含 B 团队患者预警。
  - 处理越权预警时返回 `code=403`。

  ### 场景12：健康指标类型配置联动校验

  - 前置条件：管理员账号可登录，患者账号可登录。
  - 步骤：
    1. 管理员调用 `GET /api/admin/config/indicator-types?includeDisabled=true` 获取指标列表。
    2. 管理员调用 `PUT /api/admin/config/indicator-types` 将某个指标 `enabled` 设为 `0`。
    3. 患者调用 `POST /api/patient/data` 使用该禁用指标上报。
    4. 管理员调用 `PUT /api/admin/config/indicator-types` 将该指标恢复 `enabled=1`。
  - 预期结果：
    - 步骤2更新成功，返回 `code=200`。
    - 步骤3业务返回 `code=400`，提示“指标类型未启用或不存在”。
    - 步骤4恢复成功，避免影响后续用例。

  ### 场景13：血压连续3天高阈值主动预警

  - 前置条件：患者账号可登录，医生对该患者有访问权限。
  - 步骤：
    1. 患者连续3天上报中风险以上血压（如 `150/95`，分别写入当天、前1天、前2天时间点）。
    2. 查询 `GET /api/doctor/alerts` 或数据库 `health_alert` 最新记录。
  - 预期结果：
    - 生成 `reasonCode=BP_PERSISTENT_HIGH` 的预警记录。
    - 预警 `riskLevel=MEDIUM`，且风险分高于普通单次中风险预警基线。

  ### 场景14：趋势预测预警（未超阈提前提醒）

  - 前置条件：患者存在同一指标的连续历史数据，且最近数据呈上升趋势。
  - 步骤：
    1. 患者连续上报 4-8 条逐步上升但未达到中风险阈值的数据（如血糖 7.8 -> 8.6 -> 9.4 -> 10.3）。
    2. 再次查询 `GET /api/doctor/alerts` 或 `GET /api/patient/alerts`。
  - 预期结果：
    - 接口返回 `code=200`。
    - 出现趋势预测类预警（如 `GLUCOSE_TREND_UP` / `BP_TREND_UP` / `WEIGHT_TREND_UP`）。
    - 预警级别为 `MEDIUM`，提示存在即将超阈值风险。

  ### 场景15：个性化阈值生效验证

  - 前置条件：患者账号可登录，系统已支持个性化阈值配置接口。
  - 步骤：
    1. 患者调用 `PUT /api/patient/alert-preferences` 配置某指标个性化阈值（如血压 `highRule=170/110`，`mediumRule=135/85`，`enabled=1`）。
    2. 患者上报接近新阈值的数据（如 `136/86`）。
    3. 查询 `GET /api/patient/alerts` 或医生侧预警列表。
    4. 将 `enabled` 调整为 `0` 后再次上报同类数据并对比结果。
  - 预期结果：
    - 步骤1返回 `code=200`，个性化阈值保存成功。
    - 启用个性化阈值时，预警判定按个性化阈值生效。
    - 关闭个性化阈值后，判定回退到系统规则阈值。

  ### 场景16：患者周报多图表与自定义筛选

  - 前置条件：患者存在最近一周/一月的健康上报与预警数据。
  - 步骤：
    1. 进入周报/月报页面，切换 `week/month`。
    2. 检查风险趋势图、指标饼图、指标雷达图是否渲染。
    3. 切换趋势图类型（line/bar），使用图表缩放工具。
    4. 通过“指标筛选 + 备注关键词”过滤最近上报列表。
  - 预期结果：
    - 图表渲染正常且切换平滑。
    - 缩放/还原交互有效。
    - 表格筛选结果与条件一致。

  ### 场景17：管理员监控多维分析可视化

  - 前置条件：系统有多名患者、多个群组和近14天上报数据。
  - 步骤：
    1. 进入系统监控页面。
    2. 验证近14天趋势、指标分布、群组规模、活跃患者Top四类图表。
    3. 刷新页面并验证图表数据更新。
  - 预期结果：
    - 时间/指标/群组/用户四维图表均可展示。
    - 与统计卡片及最近上报列表的数据口径一致。

### 场景9：统一错误码断言（400/401/403/404/409）

- 前置条件：管理员、医生、患者账号可登录；脚本可访问后端。
- 步骤：
  1. 触发参数校验错误（注册时 `username` 为空）验证 `code=400`。
  2. 无 token 访问管理员接口验证 `code=401`。
  3. 医生 token 访问管理员接口验证 `code=403`。
  4. 医生查询不存在患者洞察验证 `code=404`。
  5. 先注册一次唯一用户名，再重复注册同用户名验证 `code=409`。
- 预期结果：
  - 5 组断言全部通过。
  - 若任一断言失败，脚本以非 0 退出并输出失败摘要。

## 二、后端测试覆盖（单元 + 集成）

- `UserServiceImplTest`
  - `createUser_shouldCreateUserAndRoleRelation`
  - `createUser_shouldThrowWhenRoleIsAdmin`
- `HealthDataServiceImplTest`
  - `create_shouldInsertHealthData_whenBloodPressureValid`
  - `create_shouldThrow_whenBloodSugarOutOfRange`
  - `update_shouldThrow_whenDataNotOwnedByCurrentUser`
- `HealthAlertServiceImplTest`
  - `evaluateAndCreateAlert_shouldCreateHighLevelAlert_whenBloodPressureCritical`
  - `evaluateAndCreateAlert_shouldNotCreateAlert_whenIndicatorIsNormal`
- `FlywayMigrationIntegrationTest`
  - `migrateShouldSucceedWhenFeedbackReplyColumnsAlreadyExist`
  - `migrateShouldAddReplyColumnsWhenFeedbackTableExistsWithoutThem`

说明：上述 Flyway 回归测试用于防止 `feedback_message` 在“列已存在/列缺失”两种数据库状态下迁移失败。

## 三、前端测试步骤（手工）

### 1. 登录/鉴权

1. 打开 `/login`，输入正确账号密码。
2. 验证是否跳转首页，`sessionStorage` 中存在 token。
3. 伪造或清除 token 后访问受限页面，验证是否跳回登录页。
4. 同账号在新标签页重新登录，验证旧标签页收到下线提示并跳转登录页。

### 2. 患者上报页面

1. 打开“健康上报”页。
2. 分别输入合法/非法数据（如血压 `120/80` vs `abc`）。
3. 验证合法提交成功，非法数据收到错误提示。

### 3. 管理员页面

1. 打开“账号管理/系统公告/角色权限/系统监控/操作日志”页面。
2. 执行新增、编辑、删除或筛选操作。
3. 验证列表刷新与提示信息正常。

### 4. 医生页面

1. 打开“医生工作台/群组管理”页面。
2. 验证群组创建、患者管理、预警处理流程是否符合预期。

## 四、接口测试脚本

- 脚本路径：`scripts/api_test.sh`
- 用法：

```bash
chmod +x scripts/api_test.sh
BASE_URL=http://127.0.0.1:9090/api ./scripts/api_test.sh
```

- Windows PowerShell 脚本：`scripts/api_test.ps1`
- 用法：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\api_test.ps1
```

说明（Windows）：若要执行 `scripts/api_test.sh`，请使用 WSL 或 Git Bash；否则使用 `scripts/api_test.ps1`。

指定地址示例：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\api_test.ps1 -BaseUrl "http://127.0.0.1:9090/api"
```

可选（演示隔离）：通过 `RunTag` 为 CASE-11 生成隔离测试账号，避免多次演示互相影响。

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\api_test.ps1 -BaseUrl "http://127.0.0.1:9090/api" -RunTag "demo01"
```

```bash
RUN_TAG=demo01 BASE_URL=http://127.0.0.1:9090/api ./scripts/api_test.sh
```

说明：`api_test.sh` 与 `api_test.ps1` 已内置错误码断言；断言失败会返回非 0 退出码，便于 CI/批处理拦截。

补充：`api_test.ps1` 与 `api_test.sh` 已覆盖场景1-13（含场景10风险筛选、场景11团队隔离与越权处理403断言、场景12指标启停联动、场景13连续3天主动预警）。

排障补充（场景12 常见问题）：

- 若 CASE-12 出现 `401`：优先检查 CASE-5 后是否继续使用旧管理员 token。
- 若 CASE-12 出现 `500` 且报 `Data too long for column 'indicator_type'`：优先检查临时指标名长度，需保证不超过 `health_data.indicator_type` 的 `VARCHAR(20)` 上限。
- 跨平台建议：Windows 与 Linux 保持同一临时指标命名策略（短名），避免单平台通过、另一平台失败。

错误码门禁专用脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\api_assert.ps1 -BaseUrl "http://127.0.0.1:9090/api"
```

```bash
BASE_URL=http://127.0.0.1:9090/api ./scripts/api_assert.sh
```

仅跑错误码断言（CASE-9）示例：

```bash
ASSERT_ONLY=1 BASE_URL=http://127.0.0.1:9090/api ./scripts/api_test.sh
```

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\api_test.ps1 -BaseUrl "http://127.0.0.1:9090/api" -AssertOnly
```

当前 Docker 默认后端端口为 `9090`，可使用：

```bash
BASE_URL=http://127.0.0.1:9090/api ./scripts/api_test.sh
```

Flyway 迁移回归测试（Docker Maven）示例：

```bash
docker run --rm -v "${PWD}/backend:/workspace" -w /workspace maven:3.9-eclipse-temurin-17 mvn -q -Dtest=FlywayMigrationIntegrationTest test
```

说明：该用例使用 Testcontainers；若当前执行环境不可访问 Docker（例如容器内缺少 Docker Socket），测试会自动跳过，不会导致整套测试误报失败。

设计说明（关键约束与取舍）：

- Flyway 用例在容器默认测试库内执行，不再依赖 `CREATE DATABASE` 创建随机 schema，避免 CI 环境下测试账号缺少建库权限导致失败。
- 由于测试前会预建 `feedback_message` 表，schema 在迁移前属于“非空”；为保证 Flyway 可继续执行后续版本迁移，测试配置启用了 `baselineOnMigrate=true`。
- baseline 版本固定为 `10`，使迁移从 `V11` / `V12` 开始执行，正好覆盖“回复字段幂等补齐”这一回归目标。
- 用例断言聚焦在 `V11` / `V12` 执行结果（字段存在与版本记录），不改变生产迁移链路，仅作为回归保护网。
