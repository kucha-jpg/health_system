<template>
  <el-card class="page-shell role-home-v2">
    <div class="page-header">
      <div>
        <h3 class="page-title">工作首页</h3>
        <p class="page-subtitle">{{ subtitle }}</p>
      </div>
    </div>

    <div class="home-focus">{{ roleFocus }}</div>

    <div class="quick-grid">
      <article v-for="item in quickActions" :key="item.path" class="quick-card">
        <div class="quick-card-title">{{ item.title }}</div>
        <div class="quick-card-desc">{{ item.desc }}</div>
        <el-button size="small" text type="primary" @click="openQuickAction(item)">进入</el-button>
      </article>
    </div>

    <div class="notice-section">
      <div class="notice-head">
        <h4>最新公告</h4>
      </div>
      <div v-if="noticeLoading" class="notice-empty">公告加载中...</div>
      <div v-else-if="!notices.length" class="notice-empty">暂无已发布公告</div>
      <div v-else class="notice-list">
        <article v-for="item in notices" :key="item.id" class="notice-item" @click="openNotice(item)">
          <div class="notice-title-row">
            <div class="notice-title">{{ item.title }}</div>
            <el-tag v-if="isAdmin" size="small" type="info" effect="plain">{{ renderAudience(item.targetRole) }}</el-tag>
          </div>
          <div class="notice-content">{{ renderNoticeSummary(item.content) || '点击查看详情' }}</div>
          <div class="notice-readmore">点击查看全文</div>
          <div class="notice-time">发布于 {{ formatTime(item.createTime) }}</div>
        </article>
      </div>
    </div>
  </el-card>

  <el-dialog v-model="detailVisible" title="公告详情" width="680px" class="app-dialog-style" :show-close="false" align-center>
    <template v-if="selectedNotice">
      <div class="notice-detail-title-row">
        <h4 class="notice-detail-title">{{ selectedNotice.title }}</h4>
        <el-tag v-if="isAdmin" size="small" type="info" effect="plain">{{ renderAudience(selectedNotice.targetRole) }}</el-tag>
      </div>
      <div class="notice-detail-time">发布时间：{{ formatTime(selectedNotice.createTime) }}</div>
      <div class="notice-detail-content" v-html="renderNoticeHtml(selectedNotice.content)"></div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listVisibleNoticesApi } from '../api/modules'
import { authStore } from '../stores/auth'
import { richTextSummary, sanitizeRichHtml } from '../utils/richHtml'
const router = useRouter()
const role = authStore.role
const notices = ref([])
const noticeLoading = ref(false)
const detailVisible = ref(false)
const selectedNotice = ref(null)
const isAdmin = computed(() => role === 'ADMIN')

const subtitle = computed(() => {
  if (role === 'ADMIN') return '平台治理工作台'
  if (role === 'DOCTOR') return '患者干预工作台'
  return '个人健康管理工作台'
})

const roleFocus = computed(() => {
  if (role === 'ADMIN') return '今日焦点：优先处理反馈与异常日志。'
  if (role === 'DOCTOR') return '今日焦点：优先闭环高风险预警。'
  return '今日焦点：按固定时段完成健康上报。'
})

const quickActions = computed(() => {
  if (role === 'ADMIN') {
    return [
      { title: '账号管理', desc: '查看启停状态与账号结构', path: '/admin/users' },
      { title: '系统监控', desc: '跟踪平台趋势与关键指标', path: '/admin/monitor' },
      { title: '反馈消息', desc: '集中处理用户反馈与回复', path: '/admin/feedback' }
    ]
  }
  if (role === 'DOCTOR') {
    return [
      { title: '医生工作台', desc: '按风险分处理预警任务', path: '/doctor/alerts' },
      { title: '群组管理', desc: '维护协作关系与患者归属', path: '/doctor/groups' },
      { title: '反馈通道', desc: '提交问题并追踪处理结果', path: '/feedback' }
    ]
  }
  return [
    { title: '健康上报', desc: '记录每日关键健康数据', path: '/patient/report' },
    { title: '预警详情', desc: '查看风险变化与处理进度', path: '/patient/alerts' },
    { title: '周报月报', desc: '查看趋势并导出报表', path: '/patient/summary' }
  ]
})

const formatTime = (value) => {
  if (!value) return '-'
  const text = String(value).replace('T', ' ')
  return text.length > 19 ? text.slice(0, 19) : text
}

const renderAudience = (targetRole) => {
  if (targetRole === 'DOCTOR') return '仅医生'
  if (targetRole === 'PATIENT') return '仅患者'
  return '全员'
}

const renderNoticeSummary = (content) => richTextSummary(content, 90)
const renderNoticeHtml = (content) => sanitizeRichHtml(content)

const openNotice = (item) => {
  selectedNotice.value = item || null
  detailVisible.value = !!item
}

const loadNotices = async () => {
  noticeLoading.value = true
  try {
    const data = await listVisibleNoticesApi()
    notices.value = Array.isArray(data) ? data.slice(0, 5) : []
  } catch (error) {
    notices.value = []
    ElMessage.error(error?.message || '公告加载失败，请稍后重试')
  } finally {
    noticeLoading.value = false
  }
}

const openQuickAction = (item) => {
  if (!item?.path) return
  router.push(item.path)
}

onMounted(() => {
  loadNotices()
})
</script>

<style scoped>
.role-home-v2 {
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.68), rgba(255, 255, 255, 0.48)),
    radial-gradient(circle at 10% 14%, rgba(31, 143, 114, 0.12), transparent 32%);
}

.home-focus {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  background: rgba(255, 255, 255, 0.48);
  font-size: 13px;
  color: #3f5d66;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.quick-card {
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  background: rgba(255, 255, 255, 0.52);
  padding: 10px;
}

.quick-card-title {
  font-size: 14px;
  font-weight: 700;
  color: #214c55;
}

.quick-card-desc {
  margin: 6px 0 8px;
  font-size: 12px;
  color: #5b747b;
  min-height: 34px;
}

.notice-section {
  border: 1px solid rgba(255, 255, 255, 0.74);
  border-radius: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.5);
}

.notice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.notice-head h4 {
  margin: 0;
  font-size: 14px;
  color: #204f57;
}

.notice-empty {
  margin-top: 8px;
  color: #59747b;
  font-size: 13px;
}

.notice-list {
  margin-top: 8px;
  display: grid;
  gap: 8px;
}

.notice-item {
  border-radius: 10px;
  border: 1px solid rgba(32, 79, 87, 0.1);
  padding: 10px;
  background: rgba(255, 255, 255, 0.65);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.notice-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(15, 43, 54, 0.1);
}

.notice-title {
  font-size: 14px;
  font-weight: 700;
  color: #204f57;
}

.notice-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.notice-content {
  margin-top: 6px;
  font-size: 13px;
  color: #48656c;
  line-height: 1.6;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  white-space: pre-wrap;
}

.notice-readmore {
  margin-top: 4px;
  font-size: 12px;
  color: #5c8ea0;
}

.notice-time {
  margin-top: 6px;
  font-size: 12px;
  color: #7a9096;
}

.notice-detail-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.notice-detail-title {
  margin: 0;
  font-size: 18px;
  color: #18363e;
}

.notice-detail-time {
  margin-top: 10px;
  font-size: 12px;
  color: #70858b;
}

.notice-detail-content {
  margin-top: 12px;
  font-size: 14px;
  line-height: 1.75;
  color: #34525a;
  white-space: pre-wrap;
}

.notice-detail-content :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  object-fit: contain;
}

.notice-detail-content :deep(figure) {
  margin: 12px 0;
  max-width: 100%;
}

.notice-detail-content :deep(figcaption) {
  margin-top: 6px;
  font-size: 12px;
  color: #6d848a;
}

@media (max-width: 900px) {
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
