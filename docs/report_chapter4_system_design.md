# 第4章 系统概要设计与详细设计

## 4.1 前端功能模块

### 4.1.1 模块详细划分

前端采用 Vue 3 + Vue Router + Axios + Element Plus 实现，整体按“基础能力层、角色业务层、公共支撑层”分层组织。

1. 基础能力层
- 认证与会话模块：登录、注册、登录态恢复、跨标签互斥登录提醒。
- 路由与鉴权模块：路由注册、路由守卫、基于角色的页面访问控制。
- 统一网络请求模块：请求拦截器注入令牌、响应统一解包、401 自动退回登录页。

2. 角色业务层
- 患者端模块：个人档案、健康上报、历史数据、预警详情、个性化阈值、周报月报、反馈通道。
- 医生端模块：预警工作台、群组管理、患者洞察、反馈通道。
- 管理员端模块：账号管理、系统公告、预警规则、群组治理、角色权限、系统监控、操作日志、反馈消息。

3. 公共支撑层
- 主框架与导航模块：侧边栏菜单按角色动态显示，顶部栏提供主题切换、返回与退出。
- 主题与样式模块：支持主题切换，统一品牌变量与角色视觉区分。
- 首页总览模块：按角色聚合关键指标，形成“工作入口 + 运行态摘要”的组合首页。

4. 前端模块结构关系
- 路由聚合：认证路由 + 应用路由统一装配。
- 视图驱动：各业务页面通过 API 模块调用后端服务。
- 状态协同：认证状态存储在 sessionStorage，广播机制用于多标签会话同步。

### 4.1.2 接口设计

前端接口按 auth、patient、doctor、admin、feedback、config 六个模块封装，统一通过 Axios 实例访问 /api 前缀。以下按统一模板给出核心接口设计。

说明：以下示例账号与业务值来自本系统初始化脚本与回归脚本（如 admin、doctor_demo_01、patient_test_01、135/88、190/120）。其中 token、id、时间戳等字段为运行时动态数据。

#### 接口一：用户登录

1. 接口说明
- 接口地址：POST /api/auth/login
- 功能描述：用户输入账号密码后完成身份认证，返回 JWT 与用户基础信息。
- 适用角色：PATIENT、DOCTOR、ADMIN

2. 请求示例

```http
POST /api/auth/login HTTP/1.1
Host: 127.0.0.1:9090
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

3. 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 登录账号 |
| password | string | 是 | 登录密码 |

4. 响应示例

```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.xxx.yyy",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "name": "系统管理员",
      "phone": "13800000000",
      "roleType": "ADMIN"
    }
  }
}
```

5. 响应参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| code | int | 业务状态码，200 表示成功 |
| msg | string | 响应消息 |
| data.token | string | JWT 访问令牌 |
| data.userInfo | object | 当前登录用户信息 |
| data.userInfo.roleType | string | 角色类型（PATIENT/DOCTOR/ADMIN） |

#### 接口二：患者健康数据上报

1. 接口说明
- 接口地址：POST /api/patient/data
- 功能描述：患者提交健康指标数据，后端落库并触发预警评估。
- 适用角色：PATIENT

2. 请求示例

```http
POST /api/patient/data HTTP/1.1
Host: 127.0.0.1:9090
Authorization: Bearer <token>
Content-Type: application/json

{
  "indicatorType": "血压",
  "value": "145/95",
  "reportTime": "2026-04-15T08:30:00",
  "remark": "晨起测量"
}
```

3. 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| indicatorType | string | 是 | 指标类型（如血压、血糖、体重、服药） |
| value | string | 是 | 指标值，格式依指标类型而定 |
| reportTime | datetime | 否 | 上报时间，不传则取服务器当前时间 |
| remark | string | 否 | 备注信息 |

4. 响应示例

```json
{
  "code": 200,
  "msg": "上报成功",
  "data": null
}
```

5. 响应参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| code | int | 业务状态码 |
| msg | string | 处理结果消息 |
| data | null | 本接口无业务数据返回 |

#### 接口三：医生预警列表查询

1. 接口说明
- 接口地址：GET /api/doctor/alerts
- 功能描述：医生按风险等级、风险分、排序方式查询可处理预警。
- 适用角色：DOCTOR

2. 请求示例

```http
GET /api/doctor/alerts?riskLevel=HIGH&minRiskScore=70&sortBy=risk_desc&pageNo=1&pageSize=20 HTTP/1.1
Host: 127.0.0.1:9090
Authorization: Bearer <token>
```

3. 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| riskLevel | string | 否 | 风险等级过滤（LOW/MEDIUM/HIGH） |
| minRiskScore | int | 否 | 最小风险分过滤 |
| sortBy | string | 否 | 排序方式：risk_desc 或 time_desc |
| pageNo | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20 |

4. 响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [
      {
        "id": 120,
        "userId": 56,
        "indicatorType": "血压",
        "value": "190/120",
        "riskScore": 92,
        "riskLevel": "HIGH",
        "status": "OPEN",
        "createTime": "2026-04-15T08:33:00"
      }
    ],
    "total": 1,
    "pageNo": 1,
    "pageSize": 20
  }
}
```

5. 响应参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| data.list | array | 预警记录列表 |
| data.total | long | 满足条件的总记录数 |
| data.pageNo | int | 当前页码 |
| data.pageSize | int | 当前分页大小 |
| data.list[].riskScore | int | 风险分（0-100） |
| data.list[].status | string | 预警状态（OPEN/CLOSED） |

#### 接口四：管理员反馈回复

1. 接口说明
- 接口地址：PUT /api/admin/feedback/reply
- 功能描述：管理员对反馈进行回复并更新处理状态。
- 适用角色：ADMIN

2. 请求示例

```http
PUT /api/admin/feedback/reply HTTP/1.1
Host: 127.0.0.1:9090
Authorization: Bearer <token>
Content-Type: application/json

{
  "id": 18,
  "replyContent": "建议晚餐后复测血糖，并连续观察三天。",
  "status": 1
}
```

3. 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | long | 是 | 反馈记录 ID |
| replyContent | string | 是 | 回复内容 |
| status | int | 是 | 处理状态（0 未处理，1 已处理） |

4. 响应示例

```json
{
  "code": 200,
  "msg": "回复成功",
  "data": null
}
```

5. 响应参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| code | int | 业务状态码 |
| msg | string | 响应消息 |
| data | null | 本接口无业务数据返回 |

### 4.1.3 流程设计

1. 前端认证与鉴权流程
1) 用户在登录页提交账号密码。
2) 前端调用 /auth/login，成功后写入 token、角色、用户名到会话存储。
3) 路由跳转至 /home，侧边菜单按角色动态渲染。
4) 每次路由切换先执行会话校验 /auth/ping；若失效则清理会话并返回登录页。
5) 每个业务请求自动携带 Bearer Token，401 时统一退登并提示原因。

2. 患者健康上报流程
1) 患者在健康上报页填写指标类型与数值。
2) 前端提交 /patient/data，成功后提示并可进入历史列表复核。
3) 在历史数据页按指标类型、时间范围分页筛选。
4) 若发现填报错误，可执行编辑（PUT）或删除（DELETE）。
5) 患者可在预警详情和周报月报页面查看上报后的风险反馈。

3. 医生预警处置流程
1) 医生在预警工作台按风险等级、最小风险分、排序方式筛选。
2) 进入患者洞察页查看趋势数据、档案、近期预警。
3) 医生对目标预警执行处理并填写处置备注。
4) 处理结果写回后，工作台状态同步更新，实现闭环。

4. 管理员治理流程
1) 管理员在账号管理中维护用户生命周期（新增、编辑、启停）。
2) 在规则配置中维护预警规则、公告、指标类型。
3) 在系统监控页查看总体运行态，在日志页进行审计追踪。
4) 在反馈消息页执行筛选、批量状态流转与回复，完成运维闭环。

## 4.2 后台功能模块

### 4.2.1 模块详细划分

后端采用 Spring Boot 分层架构，按“接入层、业务层、领域能力层、数据层、横切能力层”设计。

1. 接入层（Controller）
- 认证接入：AuthController。
- 患者接入：PatientController。
- 医生接入：DoctorController。
- 管理员接入：AdminUser、AdminRole、AdminConfig、AdminMonitor、AdminLog、AdminFeedback。
- 公共接入：FeedbackController、NoticeController。

2. 业务层（Service）
- 账号与权限：AuthService、UserService、RoleService。
- 患者业务：PatientArchiveService、HealthDataService、PatientReportService、PatientAlertPreferenceService。
- 医生业务：HealthAlertService、DoctorGroupService、DoctorPatientInsightService。
- 平台治理：OperationLogService、FeedbackMessageService、SystemNoticeService、AlertRuleService、HealthIndicatorTypeService。

3. 领域能力层
- 预警引擎：AlertEvaluationEngine + 多指标 Evaluator（血压、血糖、体重）。
- 风险评分：RiskScoreSupport 进行风险分与风险等级映射。
- 医生可达性：DoctorAccessSupport 统一校验医生是否可访问目标患者/群组。

4. 数据层
- MyBatis-Plus Mapper 管理实体持久化。
- 关键实体包括 User、Role、HealthData、HealthAlert、DoctorGroup、PatientArchive、FeedbackMessage、AlertRule、SystemNotice 等。

5. 横切能力层
- 安全鉴权：SecurityConfig + JwtAuthenticationFilter + JwtUtils。
- 限流防护：登录接口限流过滤器。
- 缓存体系：按业务域配置缓存键，写操作主动失效，读操作命中缓存。
- 审计日志：关键治理接口支持操作日志记录和导出。

### 4.2.2 接口设计

后端接口统一采用 ApiResponse(code, msg, data) 结构，以下按统一模板说明关键接口。实际接口仍按 RESTful 资源风格分布在认证、患者、医生、管理员与公共模块中。

说明：本节请求示例中的账号与指标值取自系统真实初始化与测试脚本；响应中的 token、id、createTime 会因运行环境而变化。

#### 接口一：会话校验

1. 接口说明
- 接口地址：GET /api/auth/ping
- 功能描述：用于前端路由守卫与轮询检查当前登录态是否有效。
- 适用角色：已登录用户

2. 请求示例

```http
GET /api/auth/ping HTTP/1.1
Host: 127.0.0.1:9090
Authorization: Bearer <token>
```

3. 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Authorization | string | 是 | Bearer Token |

4. 响应示例

```json
{
  "code": 200,
  "msg": "ok",
  "data": null
}
```

5. 响应参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| code | int | 200 表示会话有效 |
| msg | string | 返回消息 |
| data | null | 本接口无业务数据 |

#### 接口二：患者历史数据分页查询

1. 接口说明
- 接口地址：GET /api/patient/data
- 功能描述：查询当前患者历史健康数据，支持指标与时间范围过滤。
- 适用角色：PATIENT

2. 请求示例

```http
GET /api/patient/data?indicator_type=血压&timeRange=week&pageNo=1&pageSize=20 HTTP/1.1
Host: 127.0.0.1:9090
Authorization: Bearer <token>
```

3. 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| indicator_type | string | 否 | 指标类型过滤 |
| timeRange | string | 否 | 时间范围 day/week/month |
| pageNo | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 20 |

4. 响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [
      {
        "id": 311,
        "userId": 56,
        "indicatorType": "血压",
        "value": "135/88",
        "reportTime": "2026-04-14T07:20:00",
        "remark": "api test rt_demo01"
      }
    ],
    "total": 26,
    "pageNo": 1,
    "pageSize": 20
  }
}
```

5. 响应参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| data.list | array | 健康数据列表 |
| data.total | long | 总记录数 |
| data.pageNo | int | 当前页码 |
| data.pageSize | int | 每页大小 |
| data.list[].indicatorType | string | 指标类型 |
| data.list[].value | string | 指标值 |

#### 接口三：医生处理预警

1. 接口说明
- 接口地址：POST /api/doctor/alerts/{id}/handle
- 功能描述：医生对 OPEN 状态预警进行处理并关闭。
- 适用角色：DOCTOR

2. 请求示例

```http
POST /api/doctor/alerts/120/handle HTTP/1.1
Host: 127.0.0.1:9090
Authorization: Bearer <token>
Content-Type: application/json

{
  "handleRemark": "已电话随访，建议减少高糖摄入。"
}
```

3. 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | long | 是 | 预警记录 ID（路径参数） |
| handleRemark | string | 否 | 处理备注 |

4. 响应示例

```json
{
  "code": 200,
  "msg": "处理成功",
  "data": null
}
```

5. 响应参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| code | int | 业务状态码 |
| msg | string | 处理结果说明 |
| data | null | 本接口无业务数据返回 |

#### 接口四：管理员配置预警规则

1. 接口说明
- 接口地址：PUT /api/admin/config/alert-rules
- 功能描述：管理员修改某指标预警阈值与启用状态，影响后续预警评估。
- 适用角色：ADMIN

2. 请求示例

```http
PUT /api/admin/config/alert-rules HTTP/1.1
Host: 127.0.0.1:9090
Authorization: Bearer <token>
Content-Type: application/json

{
  "id": 1,
  "indicatorType": "血压",
  "highRule": "180/120",
  "mediumRule": "140/90",
  "enabled": 1
}
```

3. 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | long | 是 | 规则主键 |
| indicatorType | string | 是 | 指标类型 |
| highRule | string | 是 | 高风险阈值 |
| mediumRule | string | 否 | 中风险阈值 |
| enabled | int | 是 | 启用状态（0 禁用，1 启用） |

4. 响应示例

```json
{
  "code": 200,
  "msg": "更新成功",
  "data": null
}
```

5. 响应参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| code | int | 业务状态码 |
| msg | string | 更新结果消息 |
| data | null | 本接口无业务数据返回 |

#### 接口五：管理员分页查询操作日志

1. 接口说明
- 接口地址：GET /api/admin/logs/page
- 功能描述：按关键字、角色、成功状态、时间范围进行操作日志分页检索。
- 适用角色：ADMIN

2. 请求示例

```http
GET /api/admin/logs/page?keyword=LOGIN&roleType=DOCTOR&success=1&pageNo=1&pageSize=20 HTTP/1.1
Host: 127.0.0.1:9090
Authorization: Bearer <token>
```

3. 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| keyword | string | 否 | 关键字（用户名/消息） |
| roleType | string | 否 | 角色类型 |
| success | int | 否 | 结果过滤（0 失败，1 成功） |
| startTime | datetime | 否 | 开始时间，格式 yyyy-MM-dd HH:mm:ss |
| endTime | datetime | 否 | 结束时间，格式 yyyy-MM-dd HH:mm:ss |
| pageNo | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量 |

4. 响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 901,
        "username": "doctor_demo_01",
        "roleType": "DOCTOR",
        "requestMethod": "POST",
        "requestUri": "/api/auth/login",
        "success": 1,
        "message": "LOGIN_SUCCESS",
        "createTime": "2026-04-15T08:20:00"
      }
    ],
    "total": 135,
    "pageNo": 1,
    "pageSize": 20
  }
}
```

5. 响应参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| data.records | array | 日志记录列表 |
| data.total | long | 总记录数 |
| data.pageNo | int | 当前页码 |
| data.pageSize | int | 每页大小 |
| data.records[].requestUri | string | 请求路径 |
| data.records[].success | int | 执行结果（0/1） |

### 4.2.3 流程设计

1. 后台认证授权流程
1) 用户访问登录接口，服务端验证账号密码与账号状态。
2) 登录成功后增加 loginVersion 并签发 JWT。
3) 后续请求经 JWT 过滤器解析身份、角色、登录版本。
4) 版本不一致或账号禁用则立即返回 401。
5) Security 配置按 URL 前缀进行角色鉴权，越权请求返回 403。

2. 健康数据上报与预警生成流程
1) 患者提交健康数据，服务层先进行指标类型可用性与格式校验。
2) 数据持久化后触发预警引擎评估。
3) 引擎按指标类型匹配对应 Evaluator，并优先读取患者个性化阈值。
4) 若触发风险规则则计算 riskScore 与 riskLevel，生成或更新 OPEN 预警。
5) 写操作会联动失效相关缓存，保障患者列表、医生工作台、报表视图数据一致。

3. 医生预警处理流程
1) 医生查询 OPEN 预警时，系统先解析其可访问患者集合。
2) 仅返回其群组范围内患者预警，并支持风险与时间排序。
3) 处理预警时再次校验可达性与状态（必须为 OPEN）。
4) 处理成功后写入处理人、处理时间、备注，并关闭预警。

4. 群组协作与患者洞察流程
1) 医生创建群组并维护医生成员、患者成员。
2) 患者洞察请求先校验医生是否对该患者有访问权。
3) 系统聚合患者基础信息、解密后的档案、趋势数据、近期预警与未处理计数。
4) 返回统一洞察结果供前端进行决策展示。

5. 管理员治理与审计流程
1) 管理员通过配置接口维护规则、公告、指标类型。
2) 规则变更后立即影响后续预警评估行为。
3) 管理员可分页检索日志并导出 CSV，支持时间区间与关键字过滤。
4) 对反馈信息执行状态流转、批量处理、回复，统计看板实时反映治理进度。

6. 报表与异步任务流程
1) 患者可同步请求周报月报，也可提交异步汇总任务。
2) 异步任务在线程池中执行，状态经历 RUNNING -> SUCCESS/FAILED。
3) 报表结果聚合上报次数、预警次数、指标分布、风险趋势，支持持续追踪。

## 本章小结

本章从前端与后台两个视角，给出了该系统在模块划分、接口定义、核心流程上的完整设计。整体设计呈现出以下特征：
- 角色边界清晰，患者、医生、管理员职责分工明确。
- 接口统一规范，具备可维护、可扩展的分层结构。
- 关键业务闭环完整，覆盖“采集-评估-干预-治理-审计”全链路。
- 安全、缓存、审计等横切能力与业务流程深度融合，满足医疗健康场景对稳定性与可追溯性的要求。
