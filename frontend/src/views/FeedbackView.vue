<template>
  <el-card class="page-shell fade-in-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">反馈通道</h3>
        <p class="page-subtitle">提交与跟踪反馈处理</p>
      </div>
      <div class="page-actions">
        <el-button @click="reloadFromStart">刷新</el-button>
      </div>
    </div>

    <div class="soft-tip">支持按时间范围筛选。</div>

    <el-form label-width="80px" class="feedback-form">
      <el-form-item label="反馈内容">
        <el-input v-model="content" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请输入你遇到的问题、建议或改进想法" />
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="range"
          class="w-360"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
        <el-button class="filter-btn" @click="reloadFromStart">筛选</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">提交反馈</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="content" label="反馈内容" min-width="360" />
      <el-table-column label="管理员回复" min-width="320">
        <template #default="scope">
          {{ scope.row.replyContent || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="repliedTime" label="回复时间" width="180" />
      <el-table-column prop="createTime" label="提交时间" width="180" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'warning'">
            {{ scope.row.status === 1 ? '已处理' : '未处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-state">
          <div class="empty-illustration"></div>
          <div class="empty-title">暂无反馈记录</div>
          <div class="empty-desc">可先提交一条反馈，管理员处理后会在此显示回复。</div>
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
import { ElMessage } from 'element-plus'
import { createFeedbackApi, listMyFeedbackPageApi, markMyFeedbackReadApi } from '../api/modules'

const content = ref('')
const rows = ref([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)
const range = ref([])
let timer = null

const load = async () => {
  const params = {
    pageNo: pageNo.value,
    pageSize: pageSize.value
  }
  if (range.value?.length === 2) {
    params.startTime = range.value[0]
    params.endTime = range.value[1]
  }
  const res = await listMyFeedbackPageApi(params)
  rows.value = res.records || []
  total.value = res.total || 0
}

const reloadFromStart = async () => {
  pageNo.value = 1
  await load()
}

const submit = async () => {
  const text = content.value.trim()
  if (!text) {
    ElMessage.warning('反馈内容不能为空')
    return
  }
  await createFeedbackApi({ content: text })
  ElMessage.success('反馈提交成功')
  content.value = ''
  pageNo.value = 1
  await load()
}

onMounted(() => {
  load()
  markMyFeedbackReadApi().finally(() => {
    window.dispatchEvent(new Event('feedback:read'))
  })
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
.feedback-form {
  max-width: 820px;
  margin-bottom: 18px;
}

.filter-btn {
  margin-left: 8px;
}

@media (max-width: 900px) {
  .filter-btn {
    margin-left: 0;
  }
}
</style>
