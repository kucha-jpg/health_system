<template>
  <el-card class="page-shell role-home-v2">
    <div class="page-header">
      <div>
        <h3 class="page-title">工作首页</h3>
        <p class="page-subtitle">{{ subtitle }}</p>
      </div>
    </div>

    <el-row :gutter="12" class="home-metrics">
      <el-col :xs="24" :sm="8">
        <el-card shadow="never" class="metric-card">
          <div class="quick-title">当前角色</div>
          <div class="quick-value">{{ role }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="never" class="metric-card">
          <div class="quick-title">建议优先事项</div>
          <div class="quick-value">{{ priorityTask }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="never" class="metric-card">
          <div class="quick-title">操作建议</div>
          <div class="quick-value">{{ actionHint }}</div>
        </el-card>
      </el-col>
    </el-row>

    <div class="role-guide-grid">
      <div class="guide-card">
        <h4>今日建议路径</h4>
        <p>{{ flowGuide }}</p>
      </div>
      <div class="guide-card">
        <h4>效率提示</h4>
        <p>{{ efficiencyTip }}</p>
      </div>
    </div>

    <div class="soft-tip">可通过左侧菜单进入各功能模块，建议按“上报/预警/治理”链路进行日常使用。</div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { authStore } from '../stores/auth'
const role = authStore.role

const subtitle = computed(() => {
  if (role === 'ADMIN') return '面向平台治理，建议先检查账号、规则与监控态势。'
  if (role === 'DOCTOR') return '面向患者干预，建议先处理高风险预警与群组协作任务。'
  return '面向自我健康管理，建议坚持上报并关注趋势变化与预警反馈。'
})

const priorityTask = computed(() => {
  if (role === 'ADMIN') return '监控系统与处理反馈'
  if (role === 'DOCTOR') return '高风险预警闭环'
  return '健康数据上报'
})

const actionHint = computed(() => {
  if (role === 'ADMIN') return '重点关注异常日志与规则配置'
  if (role === 'DOCTOR') return '优先按风险分排序处理'
  return '建议每日固定时间上报'
})

const flowGuide = computed(() => {
  if (role === 'ADMIN') return '账号管理 -> 预警规则 -> 系统监控 -> 反馈处理'
  if (role === 'DOCTOR') return '预警工作台 -> 群组协作 -> 患者洞察 -> 反馈回访'
  return '健康上报 -> 历史数据 -> 预警详情 -> 周报月报'
})

const efficiencyTip = computed(() => {
  if (role === 'ADMIN') return '先看监控总览，再处理待办反馈，可缩短排障路径。'
  if (role === 'DOCTOR') return '先用高风险快捷视图，随后批量处理可明显提效。'
  return '固定上报时段可提升趋势分析稳定性与预警准确度。'
})
</script>

<style scoped>
.role-home-v2 {
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.68), rgba(255, 255, 255, 0.48)),
    radial-gradient(circle at 10% 14%, rgba(31, 143, 114, 0.12), transparent 32%);
}

.home-metrics {
  margin-bottom: 12px;
}

.metric-card {
  border-radius: 14px;
}

.quick-title {
  font-size: 12px;
  color: #59747b;
}

.quick-value {
  margin-top: 6px;
  font-weight: 700;
  color: #17343a;
}

.role-guide-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.guide-card {
  border-radius: 12px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.74);
  background: rgba(255, 255, 255, 0.5);
}

.guide-card h4 {
  margin: 0;
  font-size: 14px;
  color: #204f57;
}

.guide-card p {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: #48656c;
}

@media (max-width: 900px) {
  .role-guide-grid {
    grid-template-columns: 1fr;
  }
}
</style>
