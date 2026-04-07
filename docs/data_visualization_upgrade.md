# 数据分析与可视化能力扩展说明（第五点）

日期：2026-04-07

## 1. 升级目标

针对“数据分析与可视化能力仍有扩展空间（优先级：较低）”，本次升级围绕以下方向实施：

1. 丰富图表类型（折线、柱状、饼图、雷达图）
2. 增加多维度分析（时间、群组、用户、指标）
3. 支持自定义报表与数据筛选
4. 提升图表交互性与展示效果

## 2. 已落地改造

### 2.1 患者端报表扩展

页面：`frontend/src/views/PatientReportSummaryView.vue`

新增能力：

- 风险趋势图支持折线/柱状切换
- 风险趋势图增加 `dataZoom/toolbox` 交互（缩放、还原、导出图片）
- 指标分布新增饼图
- 指标分布新增雷达图
- 最近上报支持“指标 + 备注关键词”自定义筛选

### 2.2 医生端患者洞察扩展

页面：`frontend/src/views/DoctorPatientInsightView.vue`

新增能力：

- 趋势图支持 `line/bar/scatter` 切换
- 新增预警状态分布饼图（OPEN/CLOSED）
- 新增指标频次柱状图
- 明细支持备注关键词筛选
- 预警记录支持状态筛选

### 2.3 管理员端监控扩展（多维分析）

后端：`backend/src/main/java/com/health/system/service/impl/HealthAlertServiceImpl.java`

`monitorOverview` 新增返回字段：

- `dailyReportTrend`：近14天上报趋势（时间维度）
- `indicatorDistribution`：指标分布（指标维度）
- `activeUserStats`：活跃患者Top（用户维度）
- `groupStats`：群组患者规模（群组维度）

前端：`frontend/src/views/AdminMonitorView.vue`

- 新增时间趋势折线图
- 新增指标分布饼图
- 新增群组规模柱状图
- 新增活跃患者Top柱状图

## 3. 价值说明

1. 报表不再局限单一趋势线，能够从结构分布和对比关系发现问题。
2. 医生与管理员可以按不同业务视角做同屏洞察，缩短分析路径。
3. 自定义筛选减少无关信息干扰，提升分析效率和报告可用性。
4. 图表交互增强后，数据探索能力更强，更适合演示与汇报场景。
