import fs from 'node:fs/promises'
import path from 'node:path'

const projectRoot = process.cwd()
const scanDirs = ['src']
const scanFiles = ['index.html']
const allowHosts = new Set(
  String(process.env.ALLOW_EXTERNAL_HOSTS || '')
    .split(',')
    .map((item) => item.trim().toLowerCase())
    .filter(Boolean)
)

const targetExts = new Set(['.js', '.ts', '.vue', '.css', '.scss', '.less', '.html'])
const offenders = []

const shouldScan = (filePath) => {
  const ext = path.extname(filePath).toLowerCase()
  return targetExts.has(ext)
}

const listFiles = async (dir) => {
  const entries = await fs.readdir(dir, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    if (entry.name === 'node_modules' || entry.name === 'dist' || entry.name === '.git') {
      continue
    }
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      files.push(...(await listFiles(fullPath)))
    } else if (entry.isFile() && shouldScan(fullPath)) {
      files.push(fullPath)
    }
  }
  return files
}

const isAllowed = (urlText) => {
  try {
    const parsed = new URL(urlText)
    return allowHosts.has(parsed.hostname.toLowerCase())
  } catch {
    return false
  }
}

const checkFile = async (filePath) => {
  const content = await fs.readFile(filePath, 'utf8')
  const regex = /https?:\/\/[^\s'"\)\]>]+/g
  const matches = content.match(regex) || []
  for (const match of matches) {
    if (!isAllowed(match)) {
      offenders.push({ filePath, url: match })
    }
  }
}

const main = async () => {
  const files = []
  for (const dirName of scanDirs) {
    const targetDir = path.join(projectRoot, dirName)
    try {
      const stat = await fs.stat(targetDir)
      if (stat.isDirectory()) {
        files.push(...(await listFiles(targetDir)))
      }
    } catch {
      // ignore missing dir
    }
  }

  for (const fileName of scanFiles) {
    const targetFile = path.join(projectRoot, fileName)
    try {
      const stat = await fs.stat(targetFile)
      if (stat.isFile() && shouldScan(targetFile)) {
        files.push(targetFile)
      }
    } catch {
      // ignore missing file
    }
  }

  await Promise.all(files.map(checkFile))

  if (offenders.length > 0) {
    console.error('Found forbidden external resource URLs:')
    for (const item of offenders) {
      const relPath = path.relative(projectRoot, item.filePath)
      console.error(`- ${relPath}: ${item.url}`)
    }
    console.error('Build blocked. Use local assets or add host to ALLOW_EXTERNAL_HOSTS for approved exceptions.')
    process.exit(1)
  }

  console.log('External resource check passed.')
}

main().catch((err) => {
  console.error('External resource check failed unexpectedly:', err)
  process.exit(1)
})
