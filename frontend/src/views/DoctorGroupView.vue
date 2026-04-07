<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">医生群组管理</h3>
        <p class="page-subtitle">聚焦团队分工与患者归属，支持快捷管理协作关系</p>
      </div>
      <div class="page-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreateDialog">新建群组</el-button>
      </div>
    </div>

    <div class="soft-tip">
      当前共 {{ groups.length }} 个群组。建议先创建群组，再批量添加患者与协作医生。
    </div>

    <el-table :data="groups" border v-loading="loading" empty-text="暂无群组，点击右上角“新建群组”开始">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="groupName" label="群组名称" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column label="操作" width="400">
        <template #default="scope">
          <el-button link type="primary" @click="showPatients(scope.row)">查看成员</el-button>
          <el-button link type="warning" @click="showDoctors(scope.row)">协作医生</el-button>
          <el-button link type="success" @click="addPatient(scope.row)">添加患者</el-button>
          <el-button link type="info" @click="addDoctorByGroup(scope.row)">添加医生</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="createDialogVisible" title="新建群组" width="520px">
    <el-form :model="createForm" label-width="92px">
      <el-form-item label="群组名称">
        <el-input v-model="createForm.groupName" maxlength="64" show-word-limit placeholder="例如：心血管重点随访组" />
      </el-form-item>
      <el-form-item label="群组描述">
        <el-input v-model="createForm.description" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="可选，描述患者特征或管理策略" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="createGroup">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="patientDialogVisible" :title="`群组成员 - ${activeGroup?.groupName || ''}`" width="760px">
    <el-table :data="patients" border v-loading="memberLoading" empty-text="暂无患者成员">
      <el-table-column prop="id" label="患者ID" width="100" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button link type="primary" @click="goPatientInsight(scope.row)">查看档案与趋势</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <el-dialog v-model="doctorDialogVisible" :title="`协作医生 - ${activeGroup?.groupName || ''}`" width="760px">
    <div class="dialog-actions">
      <el-button type="primary" @click="addDoctor" :disabled="!activeGroup">添加协作医生</el-button>
    </div>
    <el-table :data="doctors" border v-loading="memberLoading" empty-text="暂无协作医生">
      <el-table-column prop="id" label="医生ID" width="100" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="phone" label="手机号" />
    </el-table>
  </el-dialog>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addDoctorGroupDoctorApi,
  addDoctorGroupPatientApi,
  createDoctorGroupApi,
  getDoctorGroupsApi,
  listDoctorGroupDoctorsApi,
  listDoctorGroupPatientsApi
} from '../api/modules'

const router = useRouter()
const groups = ref([])
const patients = ref([])
const doctors = ref([])
const activeGroup = ref(null)
const loading = ref(false)
const memberLoading = ref(false)
const saving = ref(false)
const createDialogVisible = ref(false)
const createForm = ref({ groupName: '', description: '' })
const patientDialogVisible = ref(false)
const doctorDialogVisible = ref(false)

const load = async () => {
  loading.value = true
  try {
    groups.value = await getDoctorGroupsApi()
  } catch (err) {
    ElMessage.error(err?.message || '群组数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  createForm.value = { groupName: '', description: '' }
  createDialogVisible.value = true
}

const createGroup = async () => {
  const groupName = String(createForm.value.groupName || '').trim()
  if (!groupName) {
    ElMessage.warning('请输入群组名称')
    return
  }
  saving.value = true
  try {
    await createDoctorGroupApi({
      groupName,
      description: String(createForm.value.description || '').trim()
    })
    ElMessage.success('群组创建成功')
    createDialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '创建群组失败')
  } finally {
    saving.value = false
  }
}

const showPatients = async (row) => {
  activeGroup.value = row
  memberLoading.value = true
  try {
    patients.value = await listDoctorGroupPatientsApi(row.id)
    patientDialogVisible.value = true
  } catch (err) {
    ElMessage.error(err?.message || '成员列表加载失败')
  } finally {
    memberLoading.value = false
  }
}

const showDoctors = async (row) => {
  activeGroup.value = row
  memberLoading.value = true
  try {
    doctors.value = await listDoctorGroupDoctorsApi(row.id)
    doctorDialogVisible.value = true
  } catch (err) {
    ElMessage.error(err?.message || '协作医生列表加载失败')
  } finally {
    memberLoading.value = false
  }
}

const addPatient = async (row) => {
  const result = await ElMessageBox.prompt('请输入患者用户ID', `添加到群组: ${row.groupName}`)
  await addDoctorGroupPatientApi(row.id, { patientUserId: Number(result.value) })
  ElMessage.success('添加成功')
  await showPatients(row)
}

const addDoctorByGroup = async (row) => {
  activeGroup.value = row
  const result = await ElMessageBox.prompt('请输入医生用户ID', `添加到群组: ${row.groupName}`)
  await addDoctorGroupDoctorApi(row.id, { doctorUserId: Number(result.value) })
  ElMessage.success('添加成功')
  await showDoctors(row)
}

const addDoctor = async () => {
  if (!activeGroup.value) return
  const result = await ElMessageBox.prompt('请输入医生用户ID', `添加到群组: ${activeGroup.value.groupName}`)
  await addDoctorGroupDoctorApi(activeGroup.value.id, { doctorUserId: Number(result.value) })
  ElMessage.success('添加成功')
  doctors.value = await listDoctorGroupDoctorsApi(activeGroup.value.id)
}

const goPatientInsight = (row) => {
  router.push(`/doctor/patients/${row.id}`)
}

onMounted(load)
</script>

<style scoped>
.dialog-actions {
  margin-bottom: 12px;
}
</style>
