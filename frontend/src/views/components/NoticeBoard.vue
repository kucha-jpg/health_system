<template>
  <el-card class="notice-section" shadow="never" v-loading="loading">
    <template #header>
      <div class="notice-header">
        <span>平台公告</span>
        <el-button v-if="showManageLink" link type="primary" @click="$router.push('/admin/notices')">去管理公告</el-button>
      </div>
    </template>

    <div v-if="list.length" class="notice-grid">
      <button
        v-for="item in list"
        :key="item.id"
        type="button"
        class="notice-card"
        @click="openNotice(item)"
      >
        <div class="notice-top">
          <strong>{{ item.title || '未命名公告' }}</strong>
          <el-tag size="small" effect="plain">{{ roleLabel(item.targetRole) }}</el-tag>
        </div>
        <p class="notice-snippet">{{ excerpt(item.content) }}</p>
        <div class="notice-time">{{ item.createTime || '-' }}</div>
      </button>
    </div>

    <el-empty v-else description="暂无可见公告" :image-size="68" />
  </el-card>

  <el-dialog
    v-model="visible"
    width="720px"
    :title="current?.title || '公告详情'"
    center
    align-center
    :close-on-click-modal="true"
    :close-on-press-escape="false"
    :show-close="false"
  >
    <div class="notice-dialog-meta">
      <el-tag effect="light">{{ roleLabel(current?.targetRole) }}</el-tag>
      <span>{{ current?.createTime || '-' }}</span>
    </div>
    <div class="notice-dialog-content" v-html="current?.content || '-'" />
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  list: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  showManageLink: { type: Boolean, default: false }
})

const visible = ref(false)
const current = ref(null)

const excerpt = (html) => String(html || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 80) || '暂无内容'

const roleLabel = (targetRole) => {
  const normalized = String(targetRole || '').trim().toUpperCase()
  if (normalized === 'DOCTOR') return '医生'
  if (normalized === 'PATIENT') return '患者'
  return '全员'
}

const openNotice = (item) => {
  current.value = item
  visible.value = true
}
</script>

<style scoped>
.notice-section { margin-bottom: 0; }
.notice-header { display: flex; align-items: center; justify-content: space-between; }
.notice-grid { display: grid; gap: 8px; grid-template-columns: 1fr; }
.notice-card {
  text-align: left; border-radius: 12px; padding: 10px; cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.74); background: rgba(255, 255, 255, 0.54);
  transition: border-color 0.2s ease, transform 0.2s ease;
}
.notice-card:hover { border-color: rgba(var(--brand-rgb), 0.65); transform: translateY(-1px); }
.notice-snippet { margin: 8px 0; font-size: 13px; color: #4d6972; line-height: 1.6; }
.notice-time { font-size: 12px; color: #69828a; }
.notice-dialog-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; color: #607e87; }
.notice-dialog-content { max-height: 56vh; overflow: auto; line-height: 1.75; }
</style>
