export class ApiError extends Error {
  constructor(message: string, public status: number) { super(message) }
}

export function query(values: Record<string, unknown>) {
  return new URLSearchParams(Object.entries(values)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => [key, String(value)])).toString()
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(path, { credentials: 'same-origin', ...options })
  const raw = await response.text()
  let body: any = raw
  try { body = raw ? JSON.parse(raw) : undefined } catch { /* 兼容纯文本响应。 */ }
  const code = body?.code ?? response.status
  if (!response.ok || (typeof code === 'number' && code >= 400)) {
    if (code === 401) window.dispatchEvent(new Event('auth-expired'))
    throw new ApiError(body?.msg || body?.message || body?.detail || (typeof body === 'string' && body) || '请求失败，请稍后重试', code)
  }
  return (body && typeof body === 'object' && 'code' in body && 'data' in body ? body.data : body) as T
}

export function json(method: string, body?: unknown): RequestInit {
  return { method, headers: { 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body) }
}

// 按 SSE 事件边界解析，保留 token 的前导空格和正文换行。
export async function consumeSse(stream: ReadableStream<Uint8Array>, receive: (data: string) => void) {
  const reader = stream.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let data: string[] = []
  function line(value: string) {
    if (value === '') {
      if (data.length) receive(data.join('\n'))
      data = []
    } else if (value.startsWith('data:')) {
      data.push(value.slice(5).replace(/^ /, ''))
    }
  }
  try {
    while (true) {
      const { done, value } = await reader.read()
      buffer += decoder.decode(value, { stream: !done })
      let newline: number
      while ((newline = buffer.indexOf('\n')) >= 0) {
        line(buffer.slice(0, newline).replace(/\r$/, ''))
        buffer = buffer.slice(newline + 1)
      }
      if (done) {
        if (buffer) line(buffer.replace(/\r$/, ''))
        line('')
        return
      }
    }
  } finally { reader.releaseLock() }
}
