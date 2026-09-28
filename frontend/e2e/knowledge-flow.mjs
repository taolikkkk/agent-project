import { chromium, expect } from '@playwright/test'
import { mkdir } from 'node:fs/promises'

// 使用独立测试库的示例员工；测试创建的文档和会话在结束时删除。
const base = process.env.APP_URL || 'http://127.0.0.1:8010'
const output = new URL('../../.verification/', import.meta.url).pathname
await mkdir(output, { recursive: true })
const browser = await chromium.launch({ channel: 'chrome', headless: true })
const context = await browser.newContext({ viewport: { width: 1440, height: 950 }, colorScheme: 'light' })
const page = await context.newPage()
const api = context.request
let docId, conversationId
const title = `知识链路验证 ${Date.now()}`
const marker = '银杉639'
const content = `# ${title}\n\n星河验证站提供汽车保养预约服务。星河验证站预约暗号是“${marker}”，营业时间为每周一至周五09:00至17:00。此内容仅用于系统联调。`
const errors = []
page.on('pageerror', e => errors.push(e.message))
try {
  const login = await api.post(`${base}/auth/staffLogin`, { data: { empId: process.env.TEST_EMP_ID || 'TEST001', password: process.env.TEST_PASSWORD || '123456' } })
  expect((await login.json()).code).toBe(200)
  await page.goto(`${base}/documents/upload`)
  await page.getByLabel('选择知识文档').setInputFiles({ name: 'knowledge-flow.md', mimeType: 'text/markdown', buffer: Buffer.from(content) })
  await page.getByLabel('文档标题', { exact: true }).fill(title)
  await page.getByLabel('文档描述', { exact: true }).fill('浏览器业务链路验证，结束后删除')
  const uploaded = page.waitForResponse(r => r.url().endsWith('/api/document/upload'))
  await page.getByRole('button', { name: '上传文档', exact: true }).click()
  const uploadResponse = await uploaded
  expect(uploadResponse.ok()).toBeTruthy()
  docId = (await uploadResponse.json()).docId
  const split = await api.post(`${base}/api/document/split/${docId}`, { params: { splitType: 'LENGTH', chunkSize: 500, overlap: 50 } })
  expect(split.ok()).toBeTruthy()
  await expect.poll(async () => (await (await api.get(`${base}/api/document/${docId}`)).json()).status, { timeout: 60000 }).toBe('VECTOR_STORED')

  // 同名文件上传新版本后，两个版本的原文和转换文件必须保持独立。
  const version = await api.post(`${base}/api/document/upload-version`, { multipart: { docId, version: '1.1.0', changelog: '检查历史文件是否独立保存', file: { name: 'knowledge-flow.md', mimeType: 'text/markdown', buffer: Buffer.from(content.replace(marker, '红枫740')) } } })
  expect(version.ok()).toBeTruthy()
  const versions = await (await api.get(`${base}/api/document/versions/${docId}`)).json()
  expect(new Set(versions.map(v => v.docUrl)).size).toBe(2)
  expect(new Set(versions.map(v => v.convertedDocUrl)).size).toBe(2)
  const original = versions.find(v => v.version === '1.0.0')
  expect(await (await api.get(original.docUrl)).text()).toContain(marker)
  expect(await (await api.get(original.convertedDocUrl)).text()).toContain(marker)
  const switched = await api.post(`${base}/api/document/switch-version`, { params: { docId, versionId: original.versionId } })
  expect(switched.ok()).toBeTruthy()

  await page.goto(`${base}/documents`)
  await page.getByRole('button', { name: title, exact: true }).click()
  await expect(page.getByText('1.1.0', { exact: true })).toBeVisible()
  await page.getByRole('tab', { name: '知识片段', exact: true }).click()
  await expect(page.locator('.segment-text').filter({ hasText: marker })).toBeVisible()
  await page.screenshot({ path: `${output}document-detail.png`, fullPage: true, animations: 'disabled' })

  await page.goto(`${base}/chat`)
  await page.getByLabel('你的问题', { exact: true }).fill('星河验证站的汽车保养预约暗号和营业时间是什么？')
  const streamed = page.waitForResponse(r => r.url().endsWith('/chat/send'))
  await page.getByRole('button', { name: '发送问题', exact: true }).click()
  const stream = await streamed
  const events = await stream.text()
  conversationId = /\[DONE\]:([^\n]+)/.exec(events)?.[1]?.trim()
  expect(conversationId).toBeTruthy()
  await expect(page.getByRole('button', { name: '停止生成' })).toHaveCount(0, { timeout: 120000 })
  await expect(page.locator('.message.assistant')).toContainText(marker)
  await page.getByRole('button', { name: '有帮助', exact: true }).click()
  await expect(page.getByRole('button', { name: '有帮助', exact: true })).toBeDisabled()
  await page.getByRole('button', { name: /查看 \d+ 条参考资料/ }).click()
  await expect(page.locator('.reference-list')).toContainText(marker)
  await page.screenshot({ path: `${output}chat-with-reference.png`, fullPage: true, animations: 'disabled' })
  await page.goto(`${base}/chat`)
  await page.locator('.conversation-select').first().click()
  await expect(page.locator('.message.assistant')).toContainText(marker)
  expect(errors).toEqual([])
  console.log(JSON.stringify({ checks: ['真实页面上传', '分段和向量索引', '同名版本原文隔离', '版本切换与片段展示', 'Qwen 流式回答', '引用展示', '回答评价', '对话历史恢复'], pageErrors: errors }, null, 2))
} catch (error) {
  await page.screenshot({ path: `${output}knowledge-flow-failure.png`, fullPage: true, animations: 'disabled' })
  throw error
} finally {
  if (conversationId) await api.delete(`${base}/chat/${encodeURIComponent(conversationId)}`)
  if (docId) await api.delete(`${base}/api/document/${docId}`)
  await browser.close()
}
