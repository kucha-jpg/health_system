# 系统规范化重构完成报告（不改功能）

## 1. 目标与约束
- 目标：在不减少功能、不改变接口行为前提下，提高代码简洁性、规范性、模块化程度。
- 约束：每轮改动后必须进行 Docker 环境编译与单元测试回归。

## 2. 已完成重构项

### 2.1 会话与安全链路
- 统一会话校验行为，强化同账号互斥登录与禁用即时失效。
- 统一 401 失败原因返回文案，前端可感知下线原因。

### 2.2 反馈模块
- 服务层去重复：抽取用户校验、状态校验、分页结果组装、批量结果组装。
- 查询参数 DTO 化：新增 `FeedbackQueryDTO`，替代控制器/服务长参数签名。
- 控制器去重：筛选/分页/导出/批量筛选调用统一查询构造流程。

### 2.3 日志模块
- 查询参数 DTO 化：新增 `OperationLogQueryDTO`，替代长参数签名。
- 导出能力规范化：导出上限、响应头、性能日志记录逻辑保持一致并结构化。

### 2.4 公共能力抽取
- CSV 工具：新增 `CsvUtils`，统一 CSV 字段转义与 UTF-8 BOM 输出。
- 安全上下文取人：新增 `RequestActor` 与 `SecurityActorUtils`，统一当前用户/角色提取。
- 查询 DTO 构造器：新增 `QueryDtoBuilders`，统一日志/反馈查询对象构建。
- 操作日志消息构造：新增 `OperationLogMessageBuilder`，统一 `log-export` 与 `feedback-batch` 消息格式。

### 2.5 代码风格归一化
- 修复多轮补丁遗留的局部缩进噪音与链式调用排版不一致。
- 删除已无使用的局部常量与辅助方法。

### 2.6 业务异常体系统一
- 新增 `BusinessException` 并接入全局异常处理器，统一业务异常出口。
- 服务层 `RuntimeException` 已替换为统一业务异常，返回文案保持不变。
- 在保持返回结构不变前提下，已完成错误码分层：`400/401/403/404/409/500`。
- 通过静态工厂方法按业务语义抛错，减少魔法数字与分散维护成本。
- 新增 `ErrorCode` 枚举统一管理状态码，并清理安全链路硬编码状态码。

## 3. 关键新增公共类
- `backend/src/main/java/com/health/system/common/CsvUtils.java`
- `backend/src/main/java/com/health/system/common/RequestActor.java`
- `backend/src/main/java/com/health/system/common/SecurityActorUtils.java`
- `backend/src/main/java/com/health/system/common/QueryDtoBuilders.java`
- `backend/src/main/java/com/health/system/common/OperationLogMessageBuilder.java`

## 4. 回归验证结果
- 编译验证：Docker Maven 编译通过。
- 单元测试：Docker Maven 测试通过。
- 验证命令（示例）：
  - `docker run --rm -v "${PWD}/backend:/workspace" -w /workspace maven:3.9-eclipse-temurin-17 mvn -q -DskipTests compile`
  - `docker run --rm -v "${PWD}/backend:/workspace" -w /workspace maven:3.9-eclipse-temurin-17 mvn -q test`

## 5. 现存技术债（未改行为）
- 业务异常、错误码分层与错误码枚举已完成；后续可继续做错误码细分（如业务子码）。
- 部分控制器方法仍较长，后续可继续按“请求对象 + 组装器”细化。
- 当前主要依赖服务层测试，建议逐步补充控制器层/集成测试。

## 6. 交付补充
- 错误码对照文档：`docs/error_code_matrix.md`
- CI 失败处理 SOP：`docs/ci_failure_sop.md`

## 7. 本次收尾增量（2026-03-13）

### 7.1 CI 稳定性增强
- 为 `quality-gate` 增加失败时上传 surefire 报告能力，便于直接下载失败明细定位。
- 关键工作流更新：`.github/workflows/quality-gate.yml` 增加 `Upload surefire reports on failure` 步骤。
- 后端测试门禁改为“两阶段执行”：先运行关键集成测试（`SecurityErrorCodeIntegrationTest`、`FlywayMigrationIntegrationTest`），再运行全量测试，提升失败定位速度与反馈质量。
- 增加每周定时巡检（`cron: 0 2 * * 1`，UTC）以提前暴露环境漂移与依赖波动风险。

### 7.2 Flyway 迁移回归测试修复
- 针对 CI 环境账号权限限制（无 `CREATE DATABASE`）调整测试策略：不再依赖创建随机库。
- 迁移回归测试改为在容器默认测试库执行，并清理目标表后重建场景。
- 针对“非空 schema 无历史表”场景，测试侧启用 baseline，版本固定为 `10`，确保回归焦点聚焦 `V11/V12`。

### 7.3 安全集成测试可维护性优化
- `SecurityErrorCodeIntegrationTest` 将大量逐字段 mapper mock 收敛为类级批量 `@MockBean(classes = {...})`。
- 保持断言语义与接口行为不变，降低测试样板代码与后续维护成本。

### 7.4 关键提交（最近）
- `27aad46` test: simplify security integration mapper mocks
- `c56bc85` docs: document flyway integration test baseline rationale
- `f8bffef` ci: upload surefire reports on gate failure
- `e1c77ab` test: baseline flyway migration integration schema
- `300ce9c` test: avoid create database in flyway migration integration test

### 7.5 质量门禁证据
- 失败修复前 run：`23037249791`（失败，定位到后端测试阶段 Flyway 用例）
- 修复后 run：`23037540040`（成功）
- 工作流增强验证 run：`23037686074`（成功）
- 测试可维护性优化验证 run：`23038094567`（成功）
- 关键测试优先策略验证 run：`23038298869`（成功）
- 定时巡检策略改动验证 run：`23038469302`（成功）

## 8. 结论
- 本轮优化已完成“可维护性提升 + 行为不变 + 全程回归验证”的目标。
- 系统当前已达到“可继续扩展、可持续重构、可证据化交付”的稳定状态。
