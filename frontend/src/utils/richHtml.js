const ALLOWED_TAGS = new Set([
  'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's',
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'ul', 'ol', 'li', 'blockquote', 'code', 'pre',
  'a', 'img', 'span', 'figure', 'figcaption'
])

const ALLOWED_ATTRS = {
  a: new Set(['href', 'target', 'rel', 'title']),
  img: new Set(['src', 'alt', 'title', 'width', 'height', 'loading']),
  '*': new Set(['style'])
}

const isSafeUrl = (value, isImage = false) => {
  if (!value) return false
  const normalized = String(value).trim().toLowerCase()
  if (normalized.startsWith('javascript:') || normalized.startsWith('vbscript:')) {
    return false
  }
  if (normalized.startsWith('data:')) {
    return isImage && normalized.startsWith('data:image/')
  }
  return normalized.startsWith('http://') || normalized.startsWith('https://') || normalized.startsWith('/') || normalized.startsWith('./') || normalized.startsWith('../')
}

export const sanitizeRichHtml = (input) => {
  const html = String(input || '')
  const parser = new DOMParser()
  const doc = parser.parseFromString(`<div>${html}</div>`, 'text/html')
  const root = doc.body.firstElementChild

  const walk = (node) => {
    const children = Array.from(node.childNodes)
    for (const child of children) {
      if (child.nodeType === Node.ELEMENT_NODE) {
        const tag = child.tagName.toLowerCase()

        if (!ALLOWED_TAGS.has(tag)) {
          const fragment = doc.createDocumentFragment()
          while (child.firstChild) {
            fragment.appendChild(child.firstChild)
          }
          child.replaceWith(fragment)
          continue
        }

        const attrs = Array.from(child.attributes)
        for (const attr of attrs) {
          const name = attr.name.toLowerCase()
          const value = attr.value
          const allowed = (ALLOWED_ATTRS[tag] && ALLOWED_ATTRS[tag].has(name)) || (ALLOWED_ATTRS['*'] && ALLOWED_ATTRS['*'].has(name))
          if (!allowed) {
            child.removeAttribute(attr.name)
            continue
          }

          if (tag === 'a' && name === 'href' && !isSafeUrl(value, false)) {
            child.removeAttribute(attr.name)
          }
          if (tag === 'img' && name === 'src' && !isSafeUrl(value, true)) {
            child.removeAttribute(attr.name)
          }
        }

        if (tag === 'a' && child.getAttribute('href')) {
          child.setAttribute('target', '_blank')
          child.setAttribute('rel', 'noopener noreferrer')
        }

        walk(child)
      } else if (child.nodeType === Node.COMMENT_NODE) {
        child.remove()
      }
    }
  }

  walk(root)
  return root.innerHTML
}

export const richTextSummary = (input, max = 120) => {
  const html = sanitizeRichHtml(input)
  const parser = new DOMParser()
  const doc = parser.parseFromString(`<div>${html}</div>`, 'text/html')
  const text = (doc.body.textContent || '').replace(/\s+/g, ' ').trim()
  if (!text) {
    return ''
  }
  if (text.length <= max) {
    return text
  }
  return `${text.slice(0, max)}...`
}
