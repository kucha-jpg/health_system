<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">健康数据上报</h3>
        <p class="page-subtitle">按指标持续记录健康状态，系统将自动分析并触发预警</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="submit">提交上报</el-button>
      </div>
    </div>

    <div class="soft-tip">填写示例：血压 120/80，血糖 6.1，体重 65，服药状态可填“已服药/未服药”。</div>

    <el-form :model="form" label-width="120px">
      <el-form-item label="指标类型">
        <el-select v-model="form.indicatorType" style="width: 100%">
          <el-option label="血压" value="血压" />
          <el-option label="血糖" value="血糖" />
          <el-option label="体重" value="体重" />
          <el-option label="服药" value="服药" />
        </el-select>
      </el-form-item>
      <el-form-item label="数值">
        <el-input v-model="form.value" placeholder="例如：120/80、6.1、65、已服药" />
      </el-form-item>
      <el-form-item label="上报时间">
        <el-date-picker v-model="form.reportTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { reportHealthDataApi } from '../api/modules'

const form = reactive({ indicatorType: '血压', value: '', reportTime: '', remark: '' })

const validate = () => {
  if (!form.value) return '数值不能为空'
  if (form.indicatorType === '血压' && !/^[1-9]\d{1,2}\/[1-9]\d{1,2}$/.test(form.value)) return '血压格式应为xx/xx'
  if (form.indicatorType === '血糖') {
    const v = Number(form.value)
    if (!(v > 0 && v <= 30)) return '血糖必须在0-30之间'
  }
  if (form.indicatorType === '体重') {
    const v = Number(form.value)
    if (!(v > 0)) return '体重必须为正数'
  }
  if (form.indicatorType === '服药' && !['已服药', '未服药', '1', '0'].includes(form.value)) return '服药仅支持 已服药/未服药/1/0'
  return ''
}

const submit = async () => {
  const err = validate()
  if (err) {
    ElMessage.error(err)
    return
  }
  await reportHealthDataApi(form)
  ElMessage.success('上报成功')
  form.value = ''
  form.remark = ''
}
</script>
