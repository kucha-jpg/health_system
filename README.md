# 健康管理系统模块（Spring Boot + Vue）

[![quality-gate](https://github.com/kucha-jpg/health_system/actions/workflows/quality-gate.yml/badge.svg)](https://github.com/kucha-jpg/health_system/actions/workflows/quality-gate.yml)

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

## 风险评分与预警分层

- 预警在规则触发后会计算 `riskScore(0-100)` 与 `riskLevel(LOW/MEDIUM/HIGH)`。
- 医生预警工作台支持按 `riskLevel`、`minRiskScore` 过滤，并支持按风险优先或按时间优先排序。
- 患者周报/月报新增 `riskTrend`（按天统计平均风险分与预警数量）。
- 预警引擎新增趋势预测能力：当历史数据呈持续上升且预测下一时点将超阈值时，可触发趋势预警（如 `BP_TREND_UP`、`GLUCOSE_TREND_UP`、`WEIGHT_TREND_UP`）。
- 新增患者个性化阈值配置：患者可按指标维护 `highRule/mediumRule`，预警判定优先读取个性化阈值。

### 个性化阈值接口

- `GET /api/patient/alert-preferences`：查看当前患者个性化阈值配置
- `PUT /api/patient/alert-preferences`：保存当前患者个性化阈值配置
  - 入参：`{ indicatorType, highRule, mediumRule, enabled }`

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

## 系统部署文档与配置说明

### 1) 后端配置（多环境）

后端已提供以下配置文件：

- `backend/src/main/resources/application.yml`（公共配置 + `spring.profiles.active`）
- `backend/src/main/resources/application-dev.yml`
- `backend/src/main/resources/application-test.yml`
- `backend/src/main/resources/application-prod.yml`

> 启动时可通过 `--spring.profiles.active=dev|test|prod` 切换环境。

#### 关键配置项

- `server.port`：服务端口
- `spring.datasource.*`：MySQL 数据库连接
- `jwt.secret` / `jwt.expiration`：JWT 密钥和过期时间

### 2) 前端配置

项目当前使用 Vite（`vite.config.js`），同时补充了 `vue.config.js`（便于统一交付要求）。

- `frontend/vite.config.js`：Vite 代理配置
- `frontend/vue.config.js`：接口代理 + 打包参数（`outputDir/assetsDir/productionSourceMap`）

### 3) 部署步骤

#### 3.1 数据库初始化

1. 登录 MySQL。
2. 执行 SQL 脚本：

```bash
mysql -uroot -p < /path/to/health_system/sql/health_system.sql
```

3. 脚本会自动创建库表并初始化管理员账号：

- 用户名：`admin`
- 密码：`123456`

#### 3.2 后端部署（Spring Boot Jar）

1. 打包：

```bash
cd /path/to/health_system/backend
mvn clean package -DskipTests
```

2. Linux 服务器运行（示例 prod）：

```bash
nohup java -jar target/health-system-1.0.0.jar --spring.profiles.active=prod > health-system.log 2>&1 &
```

3. 查看日志：

```bash
tail -f health-system.log
```

#### 3.3 前端部署（Nginx）

1. 构建前端：

```bash
cd /path/to/health_system/frontend
npm install
npm run build
```

2. 将 `dist/` 上传到 Nginx 静态目录，例如 `/usr/share/nginx/html/health-system`。
3. Nginx 配置示例：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /usr/share/nginx/html/health-system;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:9090/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

4. 重载 Nginx：

```bash
nginx -t && nginx -s reload
```

## 测试用例与接口测试脚本

- 后端单元测试：`backend/src/test/java/com/health/system/service/impl/`
  - `UserServiceImplTest`
  - `HealthDataServiceImplTest`
- 后端集成测试：`backend/src/test/java/com/health/system/`
  - `migration/FlywayMigrationIntegrationTest`（Flyway 迁移回归）
  - `security/SecurityErrorCodeIntegrationTest`（认证/错误码链路）
  - 说明：`FlywayMigrationIntegrationTest` 使用 Testcontainers，在无 Docker 环境时会自动跳过（`@Testcontainers(disabledWithoutDocker = true)`）
- 接口测试脚本：`scripts/api_test.sh`
- 错误码门禁脚本：`scripts/api_assert.ps1`、`scripts/api_assert.sh`（仅执行 400/401/403/404/409 断言）
- 前端手工测试步骤与核心场景：`docs/testing_cases.md`

说明（Windows）：执行 `*.sh` 脚本需要 WSL 或 Git Bash 环境；若仅有 PowerShell，请使用 `*.ps1` 脚本。

### 错误码断言快速执行

PowerShell（Windows）：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\api_assert.ps1 -BaseUrl "http://127.0.0.1:9090/api"
```

Bash（Linux/WSL/CI）：

```bash
BASE_URL=http://127.0.0.1:9090/api ./scripts/api_assert.sh
```

说明：若后端代码刚更新且你在使用 Docker，请先执行 `docker compose up -d --build backend`，避免旧镜像导致断言结果与源码不一致。

### CI 质量门禁（GitHub Actions）

- 工作流文件：`.github/workflows/quality-gate.yml`
- 触发条件：`main/master` 分支的 `push` 与 `pull_request`
- 门禁内容：
  1. 启动 `mysql + backend` 容器并等待后端就绪
  2. 执行后端测试（含新增集成测试）
  3. 执行 Bash 脚本静态检查（`shellcheck`）
  4. 执行接口回归脚本 `scripts/api_test.sh`（覆盖场景1-13）
  5. 执行错误码断言脚本 `scripts/api_assert.sh`
- 失败时会自动输出容器日志，便于定位问题。

## Docker 部署配置

### 新增文件

- `docker-compose.yml`：一键编排 `mysql + backend + frontend`
- `backend/Dockerfile`：Spring Boot 后端镜像（Maven 构建 + JRE 运行，内置 Maven 镜像源）
- `frontend/Dockerfile`：Vue 前端镜像（Node 运行 Vite 服务）
- `frontend/nginx/default.conf`：前端 Nginx 配置（用于非当前 Docker 运行方式，可保留）

### 快速启动

```bash
docker compose up -d --build
```

如需切换镜像源，可通过 `IMAGE_PREFIX` 覆盖默认前缀：

```bash
# Bash: 使用官方 Docker Hub
IMAGE_PREFIX=docker.io/library/ docker compose up -d --build

# Bash: 使用当前默认加速源（与未设置时一致）
IMAGE_PREFIX=docker.m.daocloud.io/library/ docker compose up -d --build
```

```powershell
# PowerShell: 使用官方 Docker Hub
$env:IMAGE_PREFIX='docker.io/library/'; docker compose up -d --build

# PowerShell: 使用当前默认加速源（与未设置时一致）
$env:IMAGE_PREFIX='docker.m.daocloud.io/library/'; docker compose up -d --build
```

如遇 `open //./pipe/dockerDesktopLinuxEngine` 错误，请先启动 Docker Desktop 再执行上面命令。

访问地址：

- 前端：`http://localhost`
- 后端：`http://localhost:9090`
- MySQL：`localhost:3307`

### 关键环境变量（backend）

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_HOST` / `DB_PORT` / `DB_NAME`
- `DB_USER` / `DB_PASSWORD`
- `JWT_SECRET` / `JWT_EXPIRATION`

当前 `docker-compose.yml` 默认使用：

- `DB_NAME=health_system`
- `DB_USER=root`
- `DB_PASSWORD=root`
- MySQL 映射端口：`3307`
- 后端映射端口：`9090`
- 前端映射端口：`80`

### 数据初始化

当前 Docker 运行方式使用 Flyway 自动迁移初始化数据库结构（`backend/src/main/resources/db/migration`）。

说明：为避免与 Flyway 迁移重复建表/加字段冲突，`docker-compose.yml` 已移除 `health_system.sql` 的自动挂载。
如需导入样例数据，请在服务启动后手动执行 SQL。

## 本次补齐功能交付清单

### 已补齐模块

- 管理员：角色权限管理（`/api/admin/roles` + `AdminRoleView`）
- 管理员：系统公告管理（`/api/admin/config/notices` + `AdminNoticeView`）
- 管理员：操作日志查询（`/api/admin/logs` + `AdminLogView`）
- 医生：群组管理（`/api/doctor/groups` + `DoctorGroupView`）
- 患者：预警详情（`/api/patient/alerts` + `PatientAlertsView`）
- 患者：周报月报（`/api/patient/reports/summary` + `PatientReportSummaryView`）

### 快速验收（Docker）

```bash
docker compose up -d --build
docker compose ps
```

也可在验收前指定镜像前缀：

```bash
IMAGE_PREFIX=docker.m.daocloud.io/library/ docker compose up -d --build
```

访问：

- 前端：`http://localhost`
- 后端：`http://localhost:9090`

推荐按 `docs/testing_cases.md` 执行分角色验收步骤。
