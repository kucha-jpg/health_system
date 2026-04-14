<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">医生群组管理</h3>
        <p class="page-subtitle">聚焦团队分工与患者归属，统一维护成员关系</p>
      </div>
      <div class="page-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreateDialog">新建群组</el-button>
      </div>
    </div>

    <div class="soft-tip">
      当前共 {{ groups.length }} 个群组。建议先维护成员，再开展随访协作。
    </div>

    <el-table :data="groups" border v-loading="loading" empty-text="暂无群组，点击右上角“新建群组”开始">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="groupName" label="群组名称" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button link type="primary" @click="openMembers(scope.row)">成员管理</el-button>
          <el-button link type="success" @click="openAddMember(scope.row)">添加成员</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="createDialogVisible" title="新建群组" width="520px">
    <el-form :model="createForm" label-width="92px">
      <el-form-item label="群组名称">
        <el-input v-model="createForm.groupName" maxlength="64" show-word-limit placeholder="例如：心血管随访组" />
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

  <el-dialog v-model="memberDialogVisible" :title="`成员管理 - ${activeGroup?.groupName || ''}`" width="860px">
    <div class="member-toolbar">
      <div class="member-count">患者 {{ patients.length }} 人</div>
      <div class="member-count">医生 {{ doctors.length }} 人</div>
      <el-button type="primary" @click="openAddMember()" :disabled="!activeGroup">添加成员</el-button>
    </div>

    <el-tabs>
      <el-tab-pane label="患者成员">
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
      </el-tab-pane>
      <el-tab-pane label="协作医生">
        <el-table :data="doctors" border v-loading="memberLoading" empty-text="暂无协作医生">
          <el-table-column prop="id" label="医生ID" width="100" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="name" label="姓名" />
          <el-table-column prop="phone" label="手机号" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>

  <el-dialog v-model="addMemberVisible" :title="`添加成员 - ${activeGroup?.groupName || ''}`" width="520px">
    <el-form :model="memberForm" label-width="90px">
      <el-form-item label="成员类型">
        <el-segmented v-model="memberForm.memberType" :options="memberTypeOptions" />
      </el-form-item>
      <el-form-item :label="memberForm.memberType === 'PATIENT' ? '患者ID' : '医生ID'">
        <el-input v-model="memberForm.userId" placeholder="请输入用户ID" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="addMemberVisible = false">取消</el-button>
      <el-button type="primary" :loading="memberSaving" @click="submitAddMember">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
const memberSaving = ref(false)
const createDialogVisible = ref(false)
const memberDialogVisible = ref(false)
const addMemberVisible = ref(false)
const createForm = ref({ groupName: '', description: '' })
const memberForm = ref({ memberType: 'PATIENT', userId: '' })
const memberTypeOptions = [
  { label: '患者', value: 'PATIENT' },
  { label: '医生', value: 'DOCTOR' }
]

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

const loadMembers = async (groupId) => {
  memberLoading.value = true
  try {
    const [patientList, doctorList] = await Promise.all([
      listDoctorGroupPatientsApi(groupId),
      listDoctorGroupDoctorsApi(groupId)
    ])
    patients.value = patientList || []
    doctors.value = doctorList || []
  } catch (err) {
    ElMessage.error(err?.message || '成员数据加载失败')
  } finally {
    memberLoading.value = false
  }
}

const openMembers = async (row) => {
  activeGroup.value = row
  memberDialogVisible.value = true
  await loadMembers(row.id)
}

const openAddMember = (row) => {
  if (row) {
    activeGroup.value = row
  }
  if (!activeGroup.value) {
    ElMessage.warning('请先选择群组')
    return
  }
  memberForm.value = { memberType: 'PATIENT', userId: '' }
  addMemberVisible.value = true
}

const submitAddMember = async () => {
  if (!activeGroup.value) return
  const userId = Number(memberForm.value.userId)
  if (!Number.isInteger(userId) || userId <= 0) {
    ElMessage.warning('请输入有效的用户ID')
    return
  }

  memberSaving.value = true
  try {
    if (memberForm.value.memberType === 'PATIENT') {
      await addDoctorGroupPatientApi(activeGroup.value.id, { patientUserId: userId })
    } else {
      await addDoctorGroupDoctorApi(activeGroup.value.id, { doctorUserId: userId })
    }
    ElMessage.success('成员添加成功')
    addMemberVisible.value = false
    await loadMembers(activeGroup.value.id)
  } catch (err) {
    ElMessage.error(err?.message || '成员添加失败')
  } finally {
    memberSaving.value = false
  }
}

const goPatientInsight = (row) => {
  router.push(`/doctor/patients/${row.id}`)
}

onMounted(load)
</script>

<style scoped>
.member-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.member-count {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  color: #4d6971;
}
</style>
