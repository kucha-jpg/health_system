<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>历史上报数据</span>
        <div>
          <el-select v-model="query.indicator_type" placeholder="指标类型" clearable style="width: 140px; margin-right: 8px">
            <el-option label="血压" value="血压" />
            <el-option label="血糖" value="血糖" />
            <el-option label="体重" value="体重" />
            <el-option label="服药" value="服药" />
          </el-select>
          <el-select v-model="query.timeRange" placeholder="时间范围" clearable style="width: 140px; margin-right: 8px">
            <el-option label="最近一天" value="day" />
            <el-option label="最近一周" value="week" />
            <el-option label="最近一月" value="month" />
          </el-select>
          <el-button @click="load">筛选</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" border>
      <el-table-column prop="indicatorType" label="指标" width="100" />
      <el-table-column prop="value" label="数值" width="140" />
      <el-table-column prop="reportTime" label="上报时间" width="180" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="remove(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="visible" title="编辑健康数据">
    <el-form :model="form" label-width="100px">
      <el-form-item label="指标"><el-input v-model="form.indicatorType" disabled /></el-form-item>
      <el-form-item label="数值"><el-input v-model="form.value" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible=false">取消</el-button>
      <el-button type="primary" @click="saveEdit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { deleteHealthDataApi, listHealthDataApi, updateHealthDataApi } from '../api/modules'

const list = ref([])
const visible = ref(false)
const query = reactive({ indicator_type: '', timeRange: '' })
const form = reactive({ id: null, indicatorType: '', value: '', reportTime: '', remark: '' })

const load = async () => {
  list.value = await listHealthDataApi(query)
}

const openEdit = (row) => {
  Object.assign(form, row)
  visible.value = true
}

const saveEdit = async () => {
  await updateHealthDataApi(form.id, form)
  ElMessage.success('更新成功')
  visible.value = false
  await load()
}

const remove = async (id) => {
  await deleteHealthDataApi(id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(load)
</script>
