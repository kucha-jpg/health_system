<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>系统操作日志</span>
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

const load = async () => {
  logs.value = await listOperationLogsApi({ limit: 100 })
}

onMounted(load)
</script>
