<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>系统公告管理</span>
        <div style="display:flex; gap:8px; align-items:center; margin-left:auto; margin-right:8px;">
          <el-input v-model="query.keyword" placeholder="标题/内容关键字" clearable style="width:220px" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
            <el-option label="发布" :value="1" />
            <el-option label="下线" :value="0" />
          </el-select>
          <el-button @click="load">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
        <el-button type="primary" @click="openDialog()">新增公告</el-button>
      </div>
    </template>

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
    </el-table>
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
const visible = ref(false)
const form = reactive({ id: null, title: '', content: '', status: 1 })
const query = reactive({ keyword: '', status: null })
let timer = null

const load = async () => {
  notices.value = await listNoticesApi({ includeOffline: true, keyword: query.keyword, status: query.status })
}

const resetQuery = async () => {
  Object.assign(query, { keyword: '', status: null })
  await load()
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
