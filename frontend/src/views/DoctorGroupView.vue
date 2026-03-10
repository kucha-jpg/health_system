<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>医生群组管理</span>
        <el-button type="primary" @click="createGroup">新建群组</el-button>
      </div>
    </template>

    <el-table :data="groups" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="groupName" label="群组名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="操作" width="280">
        <template #default="scope">
          <el-button link type="primary" @click="showPatients(scope.row)">查看成员</el-button>
          <el-button link type="success" @click="addPatient(scope.row)">添加患者</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="patientDialogVisible" title="群组成员">
    <el-table :data="patients" border>
      <el-table-column prop="id" label="患者ID" width="100" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="phone" label="手机号" />
    </el-table>
  </el-dialog>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addDoctorGroupPatientApi, createDoctorGroupApi, getDoctorGroupsApi, listDoctorGroupPatientsApi } from '../api/modules'

const groups = ref([])
const patients = ref([])
const patientDialogVisible = ref(false)

const load = async () => {
  groups.value = await getDoctorGroupsApi()
}

const createGroup = async () => {
  const nameRes = await ElMessageBox.prompt('请输入群组名称', '新建群组')
  const descRes = await ElMessageBox.prompt('请输入描述（可为空）', '新建群组')
  await createDoctorGroupApi({ groupName: nameRes.value, description: descRes.value })
  ElMessage.success('创建成功')
  await load()
}

const showPatients = async (row) => {
  patients.value = await listDoctorGroupPatientsApi(row.id)
  patientDialogVisible.value = true
}

const addPatient = async (row) => {
  const result = await ElMessageBox.prompt('请输入患者用户ID', `添加到群组: ${row.groupName}`)
  await addDoctorGroupPatientApi(row.id, { patientUserId: Number(result.value) })
  ElMessage.success('添加成功')
}

onMounted(load)
</script>
