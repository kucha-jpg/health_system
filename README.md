# 健康管理系统模块（Spring Boot + Vue）

## 技术栈
- 后端：Spring Boot 3.x + Java 17 + Spring Security 6 + JWT + MyBatis-Plus + MySQL 8
- 前端：Vue 3 + Element Plus + Axios

## 核心接口（认证与RBAC）
- `POST /api/auth/login`
  - 入参：`{ username, password }`
  - 出参：`{ code, msg, data: { token, userInfo } }`
- `POST /api/auth/register`
  - 患者自注册
  - 入参：`{ username, password, phone, name }`

- `GET /api/admin/user`：管理员查询患者/医生账号
- `POST /api/admin/user`：管理员新增患者/医生账号
- `PUT /api/admin/user`：管理员编辑账号
- `PATCH /api/admin/user/{id}/status?status=0|1`：管理员禁用/启用账号

## 患者档案与上报接口
- `GET /api/patient/archive`：查看个人档案
- `POST /api/patient/archive`：创建/保存个人档案
- `PUT /api/patient/archive`：更新个人档案
- `DELETE /api/patient/archive/{id}`：删除个人档案

- `POST /api/patient/data`：上报健康数据
  - 入参：`{ indicatorType, value, reportTime, remark }`
- `GET /api/patient/data?indicator_type=血压&timeRange=week`：查询本人上报数据（支持类型和时间范围筛选）
- `PUT /api/patient/data/{id}`：修改本人上报数据
- `DELETE /api/patient/data/{id}`：删除本人上报数据

## 数据校验规则
- 血压：格式 `xx/xx`，且每段为正数
- 血糖：必须为正数且范围 `0-30`
- 体重：必须为正数
- 服药：仅支持 `已服药/未服药/1/0`
- 所有患者接口均按当前登录用户ID做数据隔离：只能操作自己的档案和健康数据

## 权限控制逻辑
1. 登录成功后签发 JWT，前端存 `localStorage.token`。
2. Axios 请求头统一携带 `Authorization: Bearer <token>`。
3. JWT 过滤器解析 token 并写入 Spring Security 上下文。
4. URL 前缀角色拦截：
   - `/api/admin/**` -> `ADMIN`
   - `/api/doctor/**` -> `DOCTOR`
   - `/api/patient/**` -> `PATIENT`
5. 未登录返回 401，无权限返回 403。

## 前端页面
- 登录页、注册页、管理员用户管理页
- 患者个人档案编辑页
- 患者健康数据上报页
- 患者历史数据列表页（筛选/编辑/删除）
