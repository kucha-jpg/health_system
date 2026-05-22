import { ElMessageBox } from 'element-plus'

const BASE_OPTIONS = {
  center: true,
  closeOnClickModal: false,
  closeOnPressEscape: false,
  showClose: false,
  confirmButtonText: '确定'
}

export function showSuccess(msg) {
  return ElMessageBox.alert(msg, '操作成功', { ...BASE_OPTIONS, type: 'success' })
}

export function showError(msg) {
  return ElMessageBox.alert(msg || '操作失败', '错误', { ...BASE_OPTIONS, type: 'error' })
}

export function showWarning(msg) {
  return ElMessageBox.alert(msg, '提示', { ...BASE_OPTIONS, type: 'warning' })
}

export function showConfirm(msg, title = '确认操作') {
  return ElMessageBox.confirm(msg, title, {
    type: 'warning',
    center: true,
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    closeOnClickModal: false,
    closeOnPressEscape: false,
    showClose: false
  })
}
