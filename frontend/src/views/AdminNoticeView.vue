<template>
  <el-card class="page-shell fade-in-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">系统公告管理</h3>
      </div>
      <div class="page-actions">
        <el-input v-model="query.keyword" placeholder="标题/内容关键字" clearable style="width:220px" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
          <el-option label="发布" :value="1" />
          <el-option label="下线" :value="0" />
        </el-select>
        <el-select v-model="query.targetRole" placeholder="投放对象" clearable style="width:130px">
          <el-option label="全员" value="ALL" />
          <el-option label="医生" value="DOCTOR" />
          <el-option label="患者" value="PATIENT" />
        </el-select>
        <el-button @click="load">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="openDialog()">新增公告</el-button>
      </div>
    </div>

    <div class="soft-tip">当前公告数：{{ total }}，建议标题简洁明确，内容优先说明时间范围与执行动作。</div>

    <el-table :data="notices" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" width="220" />
      <el-table-column label="投放对象" width="120">
        <template #default="scope">
          <el-tag effect="plain">{{ roleLabel(scope.row.targetRole) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="内容摘要" min-width="260">
        <template #default="scope">{{ plainText(scope.row.content).slice(0, 90) || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="scope">{{ scope.row.status === 1 ? '发布' : '下线' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230">
        <template #default="scope">
          <el-button link type="info" @click="openPreview(scope.row)">预览</el-button>
          <el-button link type="primary" @click="openDialog(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="remove(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-state">
          <div class="empty-illustration"></div>
          <div class="empty-title">暂无公告数据</div>
          <div class="empty-desc">点击“新增公告”发布第一条通知。</div>
        </div>
      </template>
    </el-table>

    <div style="margin-top: 12px; display:flex; justify-content:flex-end;">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="onPageSizeChange"
        @current-change="onPageChange"
      />
    </div>
  </el-card>

  <el-dialog
    v-model="visible"
    title="公告信息"
    center
    align-center
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
  >
    <el-form :model="form" label-width="90px">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="投放对象">
        <el-select v-model="form.targetRole" style="width: 100%">
          <el-option label="全员" value="ALL" />
          <el-option label="医生" value="DOCTOR" />
          <el-option label="患者" value="PATIENT" />
        </el-select>
      </el-form-item>
      <el-form-item label="模板">
        <el-select v-model="selectedTemplate" clearable placeholder="选择公告模板" style="width: 100%" @change="applyTemplate">
          <el-option v-for="item in noticeTemplates" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="内容">
        <el-input v-model="form.content" type="textarea" :rows="10" placeholder="支持基础 HTML（段落/标题/列表/图片）" />
      </el-form-item>
      <div class="editor-tools">
        <el-button size="small" @click="insertImageBlock">插入图片模块</el-button>
        <el-button size="small" @click="openPreview(form)">预览效果</el-button>
      </div>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible=false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="previewVisible"
    title="公告预览"
    width="760px"
    center
    align-center
    :close-on-click-modal="true"
    :close-on-press-escape="false"
    :show-close="false"
  >
    <div class="notice-preview" v-html="previewNotice.content || '-'" />
  </el-dialog>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createNoticeApi, deleteNoticeApi, listNoticesApi, updateNoticeApi } from '../api/modules'

const notices = ref([])
const allNotices = ref([])
const visible = ref(false)
const previewVisible = ref(false)
const selectedTemplate = ref('')
const form = reactive({ id: null, title: '', content: '', targetRole: 'ALL', status: 1 })
const query = reactive({ keyword: '', status: null, targetRole: '' })
const previewNotice = reactive({ title: '', content: '' })
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
let timer = null

const noticeTemplates = [
  {
    value: 'maintenance',
    label: '系统维护通知',
    content: '<h3>系统维护通知</h3><p>为保障平台稳定运行，系统将于 <strong>YYYY-MM-DD 22:00 至 23:30</strong> 进行例行维护。</p><ul><li>维护期间部分功能可能暂不可用。</li><li>请提前完成关键操作并保存数据。</li></ul><p>感谢理解与支持。</p>'
  },
  {
    value: 'health-campaign',
    label: '健康活动通知',
    content: '<h3>健康管理活动上线</h3><p>本周起开展“连续 7 天打卡”主题活动，完成每日上报即可参与积分奖励。</p><ol><li>每日完成健康上报。</li><li>查看系统反馈建议。</li><li>连续达标可获得额外激励。</li></ol>'
  },
  {
    value: 'policy-update',
    label: '制度变更通知',
    content: '<h3>流程调整通知</h3><p>自 <strong>YYYY-MM-DD</strong> 起，预警处置流程更新为“24小时初审 + 48小时复核”。</p><p>请医生与患者按新流程执行，并在反馈通道确认处理结果。</p>'
  }
]

const updatePagedNotices = () => {
  const start = (pageNo.value - 1) * pageSize.value
  notices.value = allNotices.value.slice(start, start + pageSize.value)
}

const load = async () => {
  allNotices.value = await listNoticesApi({
    includeOffline: true,
    keyword: query.keyword,
    status: query.status,
    targetRole: query.targetRole
  })
  total.value = allNotices.value.length
  if ((pageNo.value - 1) * pageSize.value >= total.value && pageNo.value > 1) {
    pageNo.value = 1
  }
  updatePagedNotices()
}

const resetQuery = async () => {
  Object.assign(query, { keyword: '', status: null, targetRole: '' })
  pageNo.value = 1
  await load()
}

const onPageChange = () => {
  updatePagedNotices()
}

const onPageSizeChange = () => {
  pageNo.value = 1
  updatePagedNotices()
}

const openDialog = (row) => {
  selectedTemplate.value = ''
  Object.assign(form, { id: null, title: '', content: '', targetRole: 'ALL', status: 1 }, row || {})
  visible.value = true
}

const applyTemplate = () => {
  const target = noticeTemplates.find((item) => item.value === selectedTemplate.value)
  if (!target) return
  form.content = target.content
}

const insertImageBlock = () => {
  const imageBlock = '<p><img src="/favicon.svg" alt="公告配图" style="max-width:100%;border-radius:12px;" /></p>'
  form.content = `${form.content || ''}${form.content ? '\n' : ''}${imageBlock}`
}

const openPreview = (row) => {
  Object.assign(previewNotice, {
    title: row?.title || form.title,
    content: row?.content || form.content
  })
  previewVisible.value = true
}

const plainText = (html) => String(html || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()

const roleLabel = (targetRole) => {
  const normalized = String(targetRole || '').trim().toUpperCase()
  if (normalized === 'DOCTOR') return '医生'
  if (normalized === 'PATIENT') return '患者'
  return '全员'
}

const save = async () => {
  if (!form.title?.trim()) {
    ElMessage.error('标题不能为空')
    return
  }
  if (!form.content?.trim()) {
    ElMessage.error('内容不能为空')
    return
  }
  try {
    await ElMessageBox.confirm(form.id ? '确认修改该公告？' : '确认发布该公告？', '保存确认', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      closeOnPressEscape: false,
      showClose: false
    })
  } catch {
    return
  }
  if (form.id) {
    await updateNoticeApi(form)
  } else {
    await createNoticeApi(form)
  }
  ElMessage.success('保存成功')
  visible.value = false
  await load()
}

const remove = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该公告？', '删除确认', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      closeOnPressEscape: false,
      showClose: false
    })
  } catch {
    return
  }
  await deleteNoticeApi(id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(() => {
  load()
  timer = window.setInterval(load, 10000)
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.editor-tools {
  display: flex;
  gap: 8px;
  margin: -8px 0 12px 90px;
}

.notice-preview {
  max-height: 60vh;
  overflow: auto;
  line-height: 1.75;
}
</style>
