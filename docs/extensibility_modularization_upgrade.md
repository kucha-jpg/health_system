# 系统扩展性与模块化增强说明（第六点）

日期：2026-04-07

## 1. 目标

针对“系统扩展性与模块化程度有待进一步增强（优先级：较低）”，本次改造聚焦四项：

1. 优化模块划分，降低耦合
2. 规范接口设计，提高复用性
3. 强化分层解耦（服务层编排与规则实现分离）
4. 预留统一扩展点，支持后续功能平滑扩展

## 2. 已完成改造

### 2.1 预警规则从单体服务拆分为可插拔模块

将原本集中在 `HealthAlertServiceImpl` 中的多指标评估逻辑抽离为独立策略模块：

- 统一评估接口：`AlertEvaluator`
- 统一输入上下文：`AlertEvaluationContext`
- 统一输出模型：`AlertDecision`
- 统一评估引擎：`AlertEvaluationEngine`

实现目录：

- backend/src/main/java/com/health/system/alert/AlertEvaluator.java
- backend/src/main/java/com/health/system/alert/AlertEvaluationContext.java
- backend/src/main/java/com/health/system/alert/AlertDecision.java
- backend/src/main/java/com/health/system/alert/AlertEvaluationEngine.java

### 2.2 按指标拆分策略实现，降低跨模块耦合

新增三类指标策略实现：

- `BloodPressureAlertEvaluator`
- `BloodSugarAlertEvaluator`
- `WeightAlertEvaluator`

每个策略只处理单一指标规则，避免在主服务中出现大体量 `switch/if` 逻辑。

### 2.3 主服务角色收敛为“编排层”

`HealthAlertServiceImpl` 仅保留：

- 预警创建/更新持久化
- 预警列表分页查询
- 医生处理闭环与权限校验
- 监控统计聚合

规则判定改为委托：

- `alertEvaluationEngine.evaluate(userId, indicatorType, value)`

这样主服务不再直接依赖阈值解析与趋势预测细节，服务层边界更清晰。

### 2.4 通用能力沉淀为复用组件

抽出通用风险计算与阈值解析工具：

- `RiskScoreSupport`（风险分计算、等级映射、阈值解析）
- `IndicatorTypes`（指标常量统一维护）

减少重复代码，避免散落硬编码，提升复用性与可维护性。

## 3. 扩展方式（新增指标示例）

后续新增指标（如心率）只需：

1. 新建 `HeartRateAlertEvaluator implements AlertEvaluator`
2. 实现 `supports("心率")` 与 `evaluate(context)`
3. 如需新增阈值规则，仅扩展规则数据，不改主服务编排代码

结论：新增指标遵循“新增类优先、少改旧代码”原则，符合开闭原则。

## 4. 验证结果

- `HealthAlertServiceImplTest` 已通过（8 passed, 0 failed）
- 新增模块编译通过，主流程可用

## 5. 后续建议

1. 为每个 `AlertEvaluator` 增加独立单元测试，形成策略级测试基线。
2. 增加策略注册顺序与冲突校验，避免同指标多实现冲突。
3. 将告警阈值来源扩展为“系统规则/个人配置/模型建议”三级来源，进一步增强可扩展性。
