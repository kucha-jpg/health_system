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

## 二、后端单元测试覆盖
- `UserServiceImplTest`
  - `createUser_shouldCreateUserAndRoleRelation`
  - `createUser_shouldThrowWhenRoleIsAdmin`
- `HealthDataServiceImplTest`
  - `create_shouldInsertHealthData_whenBloodPressureValid`
  - `create_shouldThrow_whenBloodSugarOutOfRange`
  - `update_shouldThrow_whenDataNotOwnedByCurrentUser`

## 三、前端测试步骤（手工）

### 1. 登录/鉴权
1. 打开 `/login`，输入正确账号密码。
2. 验证是否跳转首页，`localStorage` 中存在 token。
3. 伪造或清除 token 后访问受限页面，验证是否跳回登录页。

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
BASE_URL=http://127.0.0.1:8080/api ./scripts/api_test.sh
```

当前 Docker 默认后端端口为 `9090`，可使用：
```bash
BASE_URL=http://127.0.0.1:9090/api ./scripts/api_test.sh
```
