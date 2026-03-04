<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo">医疗健康系统</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/home">首页</el-menu-item>
        <el-menu-item v-if="role === 'ADMIN'" index="/admin/users">账号管理</el-menu-item>
        <el-menu-item v-if="role === 'DOCTOR'" index="/home">医生工作台</el-menu-item>
        <el-menu-item v-if="role === 'PATIENT'" index="/patient/archive">个人档案</el-menu-item>
        <el-menu-item v-if="role === 'PATIENT'" index="/patient/report">健康上报</el-menu-item>
        <el-menu-item v-if="role === 'PATIENT'" index="/patient/data">历史数据</el-menu-item>
      </el-menu>
    </aside>
    <main class="main-content">
      <div class="topbar">
        <span>{{ name }}（{{ role }}）</span>
        <el-button size="small" @click="logout">退出</el-button>
      </div>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { authStore } from '../stores/auth'

const router = useRouter()
const role = authStore.role
const name = authStore.name || '用户'

const logout = () => {
  authStore.clear()
  router.push('/login')
}
</script>
