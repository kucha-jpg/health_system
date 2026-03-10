<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>系统操作日志</span>
        <div style="display:flex; gap:8px; align-items:center; margin-left:auto; margin-right:8px;">
          <el-input v-model="query.keyword" placeholder="用户/路径/信息关键字" clearable style="width:240px" />
          <el-select v-model="query.roleType" placeholder="角色" clearable style="width:120px">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="医生" value="DOCTOR" />
            <el-option label="患者" value="PATIENT" />
          </el-select>
          <el-select v-model="query.success" placeholder="结果" clearable style="width:120px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
          <el-button @click="load">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
        <el-button @click="load">刷新</el-button>
      </div>
    </template>

    <el-table :data="logs" border>
      <el-table-column prop="createTime" label="时间" width="180" />
      <el-table-column prop="username" label="用户" width="140" />
      <el-table-column prop="roleType" label="角色" width="120" />
      <el-table-column prop="requestMethod" label="方法" width="90" />
      <el-table-column prop="requestUri" label="路径" />
      <el-table-column label="结果" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.success === 1 ? 'success' : 'danger'">{{ scope.row.success === 1 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="信息" width="160" />
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listOperationLogsApi } from '../api/modules'

const logs = ref([])
const query = ref({ keyword: '', roleType: '', success: null })

const load = async () => {
  logs.value = await listOperationLogsApi({
    limit: 100,
    keyword: query.value.keyword,
    roleType: query.value.roleType,
    success: query.value.success
  })
}

const resetQuery = async () => {
  query.value = { keyword: '', roleType: '', success: null }
  await load()
}

onMounted(load)
</script>
