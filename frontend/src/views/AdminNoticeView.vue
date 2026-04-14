<template>
  <el-card class="page-shell fade-in-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">系统公告管理</h3>
        <p class="page-subtitle">统一发布平台通知，支持按状态与关键词检索</p>
      </div>
      <div class="page-actions">
        <el-input v-model="query.keyword" placeholder="标题/内容关键字" clearable style="width:220px" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
          <el-option label="发布" :value="1" />
          <el-option label="下线" :value="0" />
        </el-select>
        <el-button @click="load">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="openDialog()">新增公告</el-button>
      </div>
    </div>

    <div class="soft-tip">当前公告数：{{ total }}，建议标题简洁明确，内容优先说明时间范围与执行动作。</div>

    <el-table :data="notices" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" width="220" />
      <el-table-column prop="content" label="内容" />
      <el-table-column label="状态" width="120">
        <template #default="scope">{{ scope.row.status === 1 ? '发布' : '下线' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button link type="primary" @click="openDialog(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="remove(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-state">
          <div class="empty-illustration"></div>
          <div class="empty-title">暂无公告数据</div>
          <div class="empty-desc">点击“新增公告”发布第一条通知。</div>
        </div>
      </template>
    </el-table>

    <div style="margin-top: 12px; display:flex; justify-content:flex-end;">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="onPageSizeChange"
        @current-change="onPageChange"
      />
    </div>
  </el-card>

  <el-dialog v-model="visible" title="公告信息">
    <el-form :model="form" label-width="90px">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible=false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createNoticeApi, deleteNoticeApi, listNoticesApi, updateNoticeApi } from '../api/modules'

const notices = ref([])
const allNotices = ref([])
const visible = ref(false)
const form = reactive({ id: null, title: '', content: '', status: 1 })
const query = reactive({ keyword: '', status: null })
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
let timer = null

const updatePagedNotices = () => {
  const start = (pageNo.value - 1) * pageSize.value
  notices.value = allNotices.value.slice(start, start + pageSize.value)
}

const load = async () => {
  allNotices.value = await listNoticesApi({ includeOffline: true, keyword: query.keyword, status: query.status })
  total.value = allNotices.value.length
  if ((pageNo.value - 1) * pageSize.value >= total.value && pageNo.value > 1) {
    pageNo.value = 1
  }
  updatePagedNotices()
}

const resetQuery = async () => {
  Object.assign(query, { keyword: '', status: null })
  pageNo.value = 1
  await load()
}

const onPageChange = () => {
  updatePagedNotices()
}

const onPageSizeChange = () => {
  pageNo.value = 1
  updatePagedNotices()
}

const openDialog = (row) => {
  Object.assign(form, { id: null, title: '', content: '', status: 1 }, row || {})
  visible.value = true
}

const save = async () => {
  if (form.id) {
    await updateNoticeApi(form)
  } else {
    await createNoticeApi(form)
  }
  ElMessage.success('保存成功')
  visible.value = false
  await load()
}

const remove = async (id) => {
  await deleteNoticeApi(id)
  ElMessage.success('删除成功')
  await load()
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
