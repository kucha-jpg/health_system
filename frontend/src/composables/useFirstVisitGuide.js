import { nextTick } from 'vue'
import { ElMessageBox } from 'element-plus'

export const showFirstVisitGuide = async ({
  storageKey,
  title,
  message,
  confirmButtonText = '开始使用'
}) => {
  if (!storageKey || typeof window === 'undefined') return false
  if (window.localStorage.getItem(storageKey) === '1') return false

  await nextTick()
  await ElMessageBox.alert(message, title, {
    confirmButtonText,
    type: 'info'
  })
  window.localStorage.setItem(storageKey, '1')
  return true
}
