import { createApp } from 'vue'
import { ElMessageBox } from 'element-plus'
import App from './App.vue'
import router from './router'
import './styles/theme.css'

const messageBoxDefaults = {
	customClass: 'rule-edit-dialog app-unified-message-box',
	customStyle: {
		width: '780px',
		maxWidth: '92vw',
		marginTop: '0'
	},
	showClose: false,
	closeOnClickModal: false,
	closeOnPressEscape: false
}

const patchMessageBoxMethod = (methodName) => {
	const original = ElMessageBox[methodName]
	if (typeof original !== 'function') {
		return
	}
	ElMessageBox[methodName] = (message, title, options = {}) => {
		return original(message, title, { ...messageBoxDefaults, ...options })
	}
}

patchMessageBoxMethod('alert')
patchMessageBoxMethod('confirm')
patchMessageBoxMethod('prompt')

createApp(App).use(router).mount('#app')
