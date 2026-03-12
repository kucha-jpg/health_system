<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>预警规则管理</span>
        <el-button type="primary" @click="openDialog()">新增规则</el-button>
      </div>
    </template>

    <el-table :data="rules" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="indicatorType" label="指标类型" width="120" />
      <el-table-column prop="highRule" label="高风险阈值" width="180" />
      <el-table-column prop="mediumRule" label="中风险阈值" width="180" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.enabled === 1 ? 'success' : 'info'">{{ scope.row.enabled === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openDialog(scope.row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="visible" title="预警规则">
    <el-form :model="form" label-width="110px">
      <el-form-item label="指标类型">
        <el-select v-model="form.indicatorType" style="width:100%" :disabled="!!form.id">
          <el-option label="血压" value="血压" />
          <el-option label="血糖" value="血糖" />
          <el-option label="体重" value="体重" />
        </el-select>
      </el-form-item>
      <el-form-item label="高风险阈值">
        <el-input v-model="form.highRule" placeholder="血压示例: 180/120；血糖示例: 16.7" />
      </el-form-item>
      <el-form-item label="中风险阈值">
        <el-input v-model="form.mediumRule" placeholder="血压示例: 140/90；血糖示例: 11.1" />
      </el-form-item>
      <el-form-item label="启用状态">
        <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createAlertRuleApi, listAlertRulesApi, updateAlertRuleApi } from '../api/modules'

const rules = ref([])
const visible = ref(false)
const form = reactive({ id: null, indicatorType: '', highRule: '', mediumRule: '', enabled: 1 })

const load = async () => {
  rules.value = await listAlertRulesApi()
}

const openDialog = (row) => {
  Object.assign(form, { id: null, indicatorType: '血压', highRule: '', mediumRule: '', enabled: 1 }, row || {})
  visible.value = true
}

const save = async () => {
  if (form.id) {
    await updateAlertRuleApi(form)
  } else {
    await createAlertRuleApi(form)
  }
  ElMessage.success('保存成功')
  visible.value = false
  await load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
