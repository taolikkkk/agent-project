import { chromium, expect } from '@playwright/test'
import { mkdir } from 'node:fs/promises'
const base = process.env.APP_URL || 'http://127.0.0.1:8010'
const output = new URL('../../.verification/', import.meta.url).pathname
await mkdir(output, { recursive: true })
const browser = await chromium.launch({ channel: 'chrome', headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 950 }, deviceScaleFactor: 1, colorScheme: 'light' })
const errors = []
page.on('pageerror', error => errors.push(error.message))
const checks = []
try {
  await page.goto(`${base}/staff-login`)
  await page.getByLabel('工号', { exact: true }).fill('TEST001')
  await page.getByLabel('密码', { exact: true }).fill('123456')
  await page.screenshot({ path: `${output}login-light.png`, fullPage: true, animations: 'disabled' })
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL('**/documents')
  await page.getByRole('heading', { name: '文档管理', exact: true }).waitFor()
  await expect(page.locator('.el-skeleton')).toHaveCount(0)
  await page.screenshot({ path: `${output}documents-light.png`, fullPage: true, animations: 'disabled' })
  checks.push('员工登录与文档列表')
  await page.getByRole('link', { name: '知识文档上传', exact: true }).click()
  await page.getByLabel('文档标题', { exact: true }).fill('界面验证示例')
  await page.getByRole('button', { name: '上传文档', exact: true }).click()
  await page.getByRole('alert').filter({ hasText: '请先选择文件' }).waitFor()
  await page.screenshot({ path: `${output}upload-light.png`, fullPage: true, animations: 'disabled' })
  checks.push('上传表单与输入校验')
  await page.getByRole('link', { name: 'AI 对话助手', exact: true }).click()
  await page.getByRole('heading', { name: '今天，想了解些什么？' }).waitFor()
  await page.screenshot({ path: `${output}chat-light.png`, fullPage: true, animations: 'disabled' })
  await page.getByRole('button', { name: '切换深色模式' }).click()
  await page.screenshot({ path: `${output}chat-dark.png`, fullPage: true, animations: 'disabled' })
  checks.push('问答界面与深浅主题')
  await page.getByRole('link', { name: '问答缓存审核', exact: true }).click()
  await expect(page.locator('.el-skeleton')).toHaveCount(0)
  await expect(page.getByRole('heading', { name: '问答缓存审核', exact: true })).toBeVisible()
  await page.screenshot({ path: `${output}cache-dark.png`, fullPage: true, animations: 'disabled' })
  checks.push('问答缓存审核页面')
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto(`${base}/chat`)
  await page.getByRole('heading', { name: '今天，想了解些什么？' }).waitFor()
  await page.screenshot({ path: `${output}chat-mobile-dark.png`, fullPage: true, animations: 'disabled' })
  if (await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)) throw new Error('移动端页面横向溢出')
  await page.getByRole('button', { name: '打开导航' }).click()
  await page.getByRole('link', { name: '文档管理', exact: true }).click()
  await page.getByRole('heading', { name: '文档管理', exact: true }).waitFor()
  checks.push('移动端导航与布局')
  if (errors.length) throw new Error(errors.join('\n'))
  console.log(JSON.stringify({ checks, pageErrors: errors, screenshots: output }, null, 2))
} catch (error) {
  await page.screenshot({ path: `${output}failure.png`, fullPage: true, animations: 'disabled' })
  console.error(await page.locator('body').innerText())
  throw error
} finally { await browser.close() }
