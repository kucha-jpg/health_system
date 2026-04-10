<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">健康数据上报</h3>
        <p class="page-subtitle">记录每日健康数据</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="submit">提交上报</el-button>
      </div>
    </div>

    <div class="info-strip">
      <div>
        <div class="info-strip-title">保持连续上报可提升预警准确性</div>
        <div class="info-strip-desc">血压示例 120/80，血糖示例 6.1，体重示例 65。</div>
      </div>
      <el-tag effect="light">当前指标：{{ form.indicatorType }}</el-tag>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-label">上报时间</div>
        <div class="kpi-value">{{ form.reportTime ? '已设置' : '未设置' }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">录入状态</div>
        <div class="kpi-value">{{ form.value ? '待提交' : '待填写' }}</div>
      </div>
    </div>

    <el-card class="section-card" shadow="never">
      <template #header>当日健康数据</template>
      <el-form :model="form" label-width="120px">
        <el-form-item label="指标类型">
          <el-select v-model="form.indicatorType" class="w-full">
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
          <el-date-picker v-model="form.reportTime" class="w-full" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <div class="page-actions">
        <el-button plain @click="fillNow">填入当前时间</el-button>
        <el-button plain @click="fillSample">填入示例值</el-button>
      </div>
    </el-card>
  </el-card>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { reportHealthDataApi } from '../api/modules'

const form = reactive({ indicatorType: '血压', value: '', reportTime: '', remark: '' })

const fillNow = () => {
  const pad = (n) => String(n).padStart(2, '0')
  const d = new Date()
  form.reportTime = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const fillSample = () => {
  if (form.indicatorType === '血压') form.value = '120/80'
  if (form.indicatorType === '血糖') form.value = '6.1'
  if (form.indicatorType === '体重') form.value = '65'
  if (form.indicatorType === '服药') form.value = '已服药'
}

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
