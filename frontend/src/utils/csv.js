export const csvEscape = (value) => {
  const text = String(value ?? '')
  if (text.includes(',') || text.includes('"') || text.includes('\n')) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

export const downloadCsv = (lines, filename) => {
  const csvText = `﻿${lines.join('\n')}`
  const blob = new Blob([csvText], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

export const downloadObjectsCsv = (objects, columns, filename) => {
  const header = columns.map(c => csvEscape(c.label)).join(',')
  const rows = objects.map(obj => columns.map(c => csvEscape(obj[c.key] ?? '')).join(','))
  downloadCsv([header, ...rows], filename)
}
