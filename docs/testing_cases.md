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

指定地址示例：
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\api_test.ps1 -BaseUrl "http://127.0.0.1:9090/api"
```

说明：`api_test.sh` 与 `api_test.ps1` 已内置错误码断言；断言失败会返回非 0 退出码，便于 CI/批处理拦截。

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
