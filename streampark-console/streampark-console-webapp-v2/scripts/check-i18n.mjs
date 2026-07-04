import { chromium } from 'playwright'

const base = process.argv[2] || 'http://localhost:10002'
const browser = await chromium.launch()
const context = await browser.newContext()
await context.addInitScript(() => {
  localStorage.setItem('STREAMPARK__LOCALE__', JSON.stringify({
    value: { showPicker: true, locale: 'zh_CN', fallback: 'en', availableLocales: ['zh_CN', 'en'] },
    time: Date.now(),
    expire: null,
  }))
  localStorage.setItem('app-store', JSON.stringify({ lang: 'zh_CN' }))
})
const page = await context.newPage()

await page.goto(`${base}/#/login`, { waitUntil: 'networkidle', timeout: 60000 })
await page.locator('input').first().fill('admin')
await page.locator('input[type="password"], input').nth(1).fill('streampark')
await page.getByRole('button', { name: /sign in|登录/i }).click()
await page.waitForTimeout(8000)

const body = await page.locator('body').innerText()
const checks = {
  hasRawMenuKey: body.includes('flink.application'),
  hasZhMenu: body.includes('作业管理'),
  hasZhDashboard: body.includes('运行中的作业'),
  hasZhSearch: body.includes('作业名称'),
  hasEnDashboard: body.includes('Running Jobs'),
}

console.log('I18N_CHECKS', JSON.stringify(checks, null, 2))
console.log('BODY_SAMPLE', body.slice(0, 500).replace(/\s+/g, ' '))

await browser.close()
process.exit(checks.hasZhMenu && checks.hasZhDashboard && !checks.hasRawMenuKey ? 0 : 1)
