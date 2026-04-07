# 预警规则智能化升级说明（第三点）

日期：2026-04-07

## 1. 升级目标

针对“预警规则智能化程度有待提升（优先级：中）”，本次升级围绕以下四点落地：

1. 引入历史数据趋势分析
2. 引入简单预测机制（线性趋势预测）
3. 支持个性化阈值设置
4. 持续优化规则准确率与实用性

## 2. 已实现能力

### 2.1 趋势分析与预测预警

在原有固定阈值判定基础上，新增“趋势上升预测”判定：

- 血压：`BP_TREND_UP`
- 血糖：`GLUCOSE_TREND_UP`
- 体重：`WEIGHT_TREND_UP`

实现方式：

- 读取最近 30 天、最多 8 条历史数据。
- 使用一元线性回归计算斜率与截距，预测下一时点值。
- 当前值未超阈但预测值将达中/高风险阈值时，生成 MEDIUM 级预警，实现“提前干预”。

核心实现：

- `backend/src/main/java/com/health/system/service/impl/HealthAlertServiceImpl.java`

### 2.2 个性化阈值设置

新增患者维度个性化阈值配置能力：

- 新表：`patient_alert_preference`
- 维度：`user_id + indicator_type` 唯一
- 可配置字段：`high_rule`、`medium_rule`、`enabled`

优先级策略：

1. 若患者启用了个性化阈值，则优先使用个性化阈值
2. 否则回退系统规则表 `alert_rule`
3. 若规则缺失则回退默认阈值

实现文件：

- 迁移：`backend/src/main/resources/db/migration/V19__add_patient_alert_preference_table.sql`
- 实体：`backend/src/main/java/com/health/system/entity/PatientAlertPreference.java`
- Mapper：`backend/src/main/java/com/health/system/mapper/PatientAlertPreferenceMapper.java`
- 服务：`backend/src/main/java/com/health/system/service/impl/PatientAlertPreferenceServiceImpl.java`

### 2.3 患者端阈值接口

新增患者个人阈值查询与保存接口：

- `GET /api/patient/alert-preferences`
- `PUT /api/patient/alert-preferences`

控制器：

- `backend/src/main/java/com/health/system/controller/PatientController.java`

DTO：

- `backend/src/main/java/com/health/system/dto/PatientAlertPreferenceDTO.java`

## 3. 对业务价值的提升

1. 从“超阈后告警”升级为“趋势预测+提前提醒”。
2. 实现患者差异化阈值配置，增强预警适应性。
3. 与原有固定规则兼容，平滑升级，避免破坏已有流程。

## 4. 后续可继续优化方向

1. 引入更丰富特征（波动率、日内时段、服药事件）提升预测准确率。
2. 在医生端增加“推荐阈值”辅助功能，由系统给出个性化建议。
3. 增加阈值变更审计日志与效果评估报表（命中率、误报率）。
