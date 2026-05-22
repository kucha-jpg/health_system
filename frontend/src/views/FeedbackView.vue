<template>
  <el-card class="page-shell fade-in-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">反馈通道</h3>
      </div>
      <div class="page-actions">
        <el-tag effect="plain" size="small">共 {{ total }} 条</el-tag>
        <el-button @click="markRead">标记已读</el-button>
        <el-button @click="reloadFromStart">刷新</el-button>
      </div>
    </div>

    <div class="soft-tip">提交反馈后，管理员将会尽快处理并回复。</div>

    <el-card class="submit-card" shadow="never">
      <div class="submit-card-header">提交反馈</div>
      <el-input
        v-model="content"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="请描述你遇到的问题、建议或改进想法"
        class="submit-textarea"
      />
      <el-button type="primary" class="submit-btn" @click="submit">提交反馈</el-button>
    </el-card>

    <div class="filter-row">
      <el-select v-model="statusFilter" class="w-130" clearable placeholder="处理状态" @change="reloadFromStart">
        <el-option label="未处理" :value="0" />
        <el-option label="已处理" :value="1" />
      </el-select>
      <span v-if="statusFilter !== '' && statusFilter !== null" class="filter-hint">
        当前筛选：{{ statusFilter === 1 ? '已处理' : '未处理' }}
      </span>
    </div>

    <el-table :data="rows" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="content" label="反馈内容" min-width="340" show-overflow-tooltip />
      <el-table-column label="管理员回复" min-width="300" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="scope.row.replyContent" class="reply-text">{{ scope.row.replyContent }}</span>
          <span v-else class="no-reply">暂未回复</span>
        </template>
      </el-table-column>
      <el-table-column prop="repliedTime" label="回复时间" width="180" />
      <el-table-column prop="createTime" label="提交时间" width="180" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'warning'" effect="plain">
            {{ scope.row.status === 1 ? '已处理' : '未处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-state">
          <div class="empty-illustration"></div>
          <div class="empty-title">暂无反馈记录</div>
          <div class="empty-desc">提交一条反馈，管理员处理后会在此显示回复。</div>
        </div>
      </template>
    </el-table>

    <div class="pager-row">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="load"
        @current-change="load"
      />
    </div>
  </el-card>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { showError, showWarning } from '../utils/message'
import { createFeedbackApi, listMyFeedbackPageApi, markMyFeedbackReadApi } from '../api/modules'

const content = ref('')
const rows = ref([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)
const statusFilter = ref(null)
let timer = null

const load = async () => {
  try {
    const params = {
      pageNo: pageNo.value,
      pageSize: pageSize.value
    }
    if (statusFilter.value !== null && statusFilter.value !== '') {
      params.status = statusFilter.value
    }
    const res = await listMyFeedbackPageApi(params)
    rows.value = res.list || []
    total.value = res.total || 0
  } catch (err) {
    showError(err?.message || '加载反馈列表失败，请稍后重试')
  }
}

const reloadFromStart = async () => {
  pageNo.value = 1
  await load()
}

const submit = async () => {
  const text = content.value.trim()
  if (!text) {
    showWarning('反馈内容不能为空')
    return
  }
  try {
    await createFeedbackApi({ content: text })
    content.value = ''
    pageNo.value = 1
    await load()
  } catch (err) {
    showError(err?.message || '提交反馈失败，请稍后重试')
  }
}

const markRead = async () => {
  try {
    await markMyFeedbackReadApi()
    window.dispatchEvent(new Event('feedback:read'))
  } catch (err) {
    showError(err?.message || '标记已读失败')
  }
}

onMounted(() => {
  load()
  timer = window.setInterval(load, 10000)
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.submit-card {
  margin-bottom: 14px;
  max-width: 600px;
}

.submit-textarea {
  max-width: 100%;
}

.submit-card-header {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-1);
  margin-bottom: 10px;
}

.submit-btn {
  margin-top: 10px;
}

.reply-text {
  color: var(--ink-1);
}

.no-reply {
  color: var(--ink-2);
  font-size: 12px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.filter-hint {
  font-size: 12px;
  color: var(--ink-2);
}
</style>
