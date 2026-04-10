<template>
  <el-card class="page-shell fade-in-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">系统公告管理</h3>
        <p class="page-subtitle">发布与检索平台公告</p>
      </div>
      <div class="page-actions">
        <el-input v-model="query.keyword" class="w-220" placeholder="标题/内容关键字" clearable />
        <el-select v-model="query.status" class="w-120" placeholder="状态" clearable>
          <el-option label="发布" :value="1" />
          <el-option label="下线" :value="0" />
        </el-select>
        <el-select v-model="query.targetRole" class="w-140" placeholder="投放对象" clearable>
          <el-option label="全员" value="ALL" />
          <el-option label="医生" value="DOCTOR" />
          <el-option label="患者" value="PATIENT" />
        </el-select>
        <el-button @click="load">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="openDialog()">新增公告</el-button>
      </div>
    </div>

    <div class="info-strip">
      <div>
        <div class="info-strip-title">公告支持按投放对象精准发布，减少无关干扰</div>
        <div class="info-strip-desc">当前筛选结果 {{ notices.length }} 条。</div>
      </div>
      <el-tag effect="light">已发布 {{ publishedCount }} 条</el-tag>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-label">已发布</div>
        <div class="kpi-value">{{ publishedCount }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">已下线</div>
        <div class="kpi-value">{{ offlineCount }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">医生定向</div>
        <div class="kpi-value">{{ doctorTargetCount }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">患者定向</div>
        <div class="kpi-value">{{ patientTargetCount }}</div>
      </div>
    </div>

    <el-table :data="notices" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" width="220" />
      <el-table-column label="投放对象" width="120">
        <template #default="scope">{{ renderTargetRole(scope.row.targetRole) }}</template>
      </el-table-column>
      <el-table-column label="内容摘要" min-width="280">
        <template #default="scope">{{ renderNoticeSummary(scope.row.content) || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="scope">{{ scope.row.status === 1 ? '发布' : '下线' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
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
  </el-card>

  <el-dialog v-model="visible" title="公告信息" width="900px" class="app-dialog-style" :show-close="false" align-center>
    <el-form :model="form" label-width="90px">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="内容">
        <div class="notice-editor-wrap">
          <div class="notice-editor-toolbar">
            <el-radio-group v-model="editorMode" size="small">
              <el-radio-button value="edit">编辑</el-radio-button>
              <el-radio-button value="preview">预览</el-radio-button>
            </el-radio-group>
            <el-button size="small" @click="insertTemplateParagraph">插入段落模板</el-button>
            <el-button size="small" @click="insertImageTag">插入图片</el-button>
            <el-button size="small" type="primary" plain @click="triggerLocalImagePick">上传本地图片</el-button>
          </div>
          <input ref="imageInputRef" type="file" accept="image/*" class="notice-image-input" @change="onLocalImageSelected" />
          <el-input
            v-if="editorMode === 'edit'"
            v-model="form.content"
            type="textarea"
            :rows="14"
            placeholder="支持 HTML 富文本，例如：<h3>标题</h3><p>正文</p><img src='图片地址' alt='示意图' />"
          />
          <div v-else class="notice-editor-preview" v-html="previewContent"></div>
        </div>
      </el-form-item>
      <el-form-item label="投放对象">
        <el-select v-model="form.targetRole" class="w-full">
          <el-option label="全员" value="ALL" />
          <el-option label="医生" value="DOCTOR" />
          <el-option label="患者" value="PATIENT" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible=false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createNoticeApi, deleteNoticeApi, listNoticesApi, updateNoticeApi } from '../api/modules'
import { richTextSummary, sanitizeRichHtml } from '../utils/richHtml'

const notices = ref([])
const visible = ref(false)
const form = reactive({ id: null, title: '', content: '', targetRole: 'ALL', status: 1 })
const query = reactive({ keyword: '', status: null, targetRole: '' })
const editorMode = ref('edit')
const imageInputRef = ref(null)
let timer = null

const previewContent = computed(() => sanitizeRichHtml(form.content || ''))
const publishedCount = computed(() => notices.value.filter((item) => item.status === 1).length)
const offlineCount = computed(() => notices.value.filter((item) => item.status !== 1).length)
const doctorTargetCount = computed(() => notices.value.filter((item) => item.targetRole === 'DOCTOR').length)
const patientTargetCount = computed(() => notices.value.filter((item) => item.targetRole === 'PATIENT').length)

const load = async () => {
  notices.value = await listNoticesApi({
    includeOffline: true,
    keyword: query.keyword,
    status: query.status,
    targetRole: query.targetRole || undefined
  })
}

const resetQuery = async () => {
  Object.assign(query, { keyword: '', status: null, targetRole: '' })
  await load()
}

const openDialog = (row) => {
  Object.assign(form, { id: null, title: '', content: '', targetRole: 'ALL', status: 1 }, row || {})
  editorMode.value = 'edit'
  visible.value = true
}

const renderTargetRole = (targetRole) => {
  if (targetRole === 'DOCTOR') return '医生'
  if (targetRole === 'PATIENT') return '患者'
  return '全员'
}

const save = async () => {
  if (!String(form.title || '').trim()) {
    ElMessage.warning('请填写公告标题')
    return
  }
  try {
    if (form.id) {
      await updateNoticeApi(form)
    } else {
      await createNoticeApi(form)
    }
    ElMessage.success('保存成功')
    visible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error?.message || '保存失败，请检查图片地址是否可访问，或改用“上传本地图片”')
  }
}

const renderNoticeSummary = (content) => richTextSummary(content, 90)

const insertTemplateParagraph = () => {
  const template = '<h3>公告标题</h3>\n<p>这里填写公告正文内容，可以分段说明时间、范围与执行要求。</p>\n<p><strong>执行时间：</strong>2026-04-09 09:00</p>'
  form.content = form.content ? `${form.content}\n${template}` : template
}

const insertImageTag = async () => {
  try {
    const result = await ElMessageBox.prompt('请输入图片地址（http/https 或 data:image）', '插入图片', {
      inputPlaceholder: '请输入可访问的图片地址',
      confirmButtonText: '插入',
      cancelButtonText: '取消'
    })
    const imageUrl = String(result.value || '').trim()
    if (!imageUrl) {
      ElMessage.warning('图片地址不能为空')
      return
    }
    const snippet = `<figure><img src="${imageUrl}" alt="公告图片" /><figcaption>图片说明</figcaption></figure>`
    form.content = form.content ? `${form.content}\n${snippet}` : snippet
  } catch {
    // user canceled
  }
}

const triggerLocalImagePick = () => {
  imageInputRef.value?.click()
}

const fileToDataUrl = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader()
  reader.onload = () => resolve(reader.result)
  reader.onerror = () => reject(new Error('图片读取失败'))
  reader.readAsDataURL(file)
})

const loadImageFromFile = (file) => new Promise((resolve, reject) => {
  const objectUrl = URL.createObjectURL(file)
  const image = new Image()
  image.onload = () => {
    URL.revokeObjectURL(objectUrl)
    resolve(image)
  }
  image.onerror = () => {
    URL.revokeObjectURL(objectUrl)
    reject(new Error('图片解析失败'))
  }
  image.src = objectUrl
})

const canvasToBlob = (canvas, type, quality) => new Promise((resolve) => {
  canvas.toBlob((blob) => resolve(blob), type, quality)
})

const compressImageToDataUrl = async (file) => {
  const image = await loadImageFromFile(file)
  const maxSide = 1600
  const ratio = Math.min(1, maxSide / Math.max(image.width, image.height))
  const targetWidth = Math.max(1, Math.round(image.width * ratio))
  const targetHeight = Math.max(1, Math.round(image.height * ratio))

  const canvas = document.createElement('canvas')
  canvas.width = targetWidth
  canvas.height = targetHeight
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('浏览器不支持图片压缩')
  }
  ctx.drawImage(image, 0, 0, targetWidth, targetHeight)

  const qualityCandidates = [0.86, 0.76, 0.66, 0.56]
  const maxBytes = 2 * 1024 * 1024
  let bestBlob = null
  for (const quality of qualityCandidates) {
    const blob = await canvasToBlob(canvas, 'image/jpeg', quality)
    if (!blob) {
      continue
    }
    bestBlob = blob
    if (blob.size <= maxBytes) {
      break
    }
  }

  if (!bestBlob) {
    throw new Error('图片压缩失败')
  }

  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => reject(new Error('压缩结果读取失败'))
    reader.readAsDataURL(bestBlob)
  })
}

const onLocalImageSelected = async (event) => {
  const file = event?.target?.files?.[0]
  if (!file) {
    return
  }
  const isImage = String(file.type || '').startsWith('image/')
  if (!isImage) {
    ElMessage.warning('请选择图片文件')
    event.target.value = ''
    return
  }

  let dataUrl = null
  try {
    if (file.size > 2 * 1024 * 1024) {
      dataUrl = await compressImageToDataUrl(file)
      ElMessage.success('图片超过 2MB，已自动压缩后插入')
    } else {
      dataUrl = await fileToDataUrl(file)
    }
  } catch (error) {
    ElMessage.error(error?.message || '图片处理失败')
    event.target.value = ''
    return
  }

  const safeSrc = String(dataUrl || '')
  if (!safeSrc.startsWith('data:image/')) {
    ElMessage.warning('仅支持图片文件')
    event.target.value = ''
    return
  }

  const snippet = `<figure><img src="${safeSrc}" alt="${file.name}" /><figcaption>${file.name}</figcaption></figure>`
  form.content = form.content ? `${form.content}\n${snippet}` : snippet
  event.target.value = ''
  ElMessage.success('图片已插入公告内容')
}

const remove = async (id) => {
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
.notice-editor-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-editor-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.notice-editor-preview {
  min-height: 320px;
  border: 1px solid rgba(32, 79, 87, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.7);
  padding: 12px;
  color: #34525a;
  line-height: 1.75;
}

.notice-image-input {
  display: none;
}

.notice-editor-preview :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  object-fit: contain;
}

.notice-editor-preview :deep(figure) {
  margin: 10px 0;
  max-width: 100%;
}

.notice-editor-preview :deep(figcaption) {
  margin-top: 6px;
  font-size: 12px;
  color: #6d848a;
}

.notice-editor-preview :deep(h1),
.notice-editor-preview :deep(h2),
.notice-editor-preview :deep(h3),
.notice-editor-preview :deep(h4) {
  margin: 0.2em 0 0.5em;
  color: #1a3d45;
}

.notice-editor-preview :deep(p) {
  margin: 0.5em 0;
}
</style>
