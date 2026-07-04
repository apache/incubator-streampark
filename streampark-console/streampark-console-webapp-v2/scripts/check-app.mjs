import { chromium } from 'playwright'

const base = process.argv[2] || 'http://localhost:10002'
const browser = await chromium.launch()
const page = await browser.newPage()
const errors = []
const failedRequests = []

page.on('pageerror', e => errors.push(`PAGE: ${e.message}`))
page.on('console', msg => {
  if (msg.type() === 'error')
    errors.push(`CONSOLE: ${msg.text()}`)
})
page.on('requestfailed', req => {
  failedRequests.push(`${req.method()} ${req.url()} — ${req.failure()?.errorText}`)
})
page.on('response', (res) => {
  if (res.status() >= 400)
    failedRequests.push(`${res.status()} ${res.url()}`)
})

await page.goto(`${base}/#/login`, { waitUntil: 'networkidle', timeout: 60000 })
await page.locator('input').first().fill('admin')
await page.locator('input[type="password"], input').nth(1).fill('streampark')
await page.getByRole('button', { name: /sign in|登录/i }).click()
await page.waitForTimeout(8000)

const bodyText = (await page.locator('body').innerText()).replace(/\s+/g, ' ')
const onLogin = page.url().includes('/login')
const onNotFound = bodyText.includes('返回首页') && bodyText.length < 50
const hasAppUi = /application|作业|Flink|Sign in/i.test(bodyText) && !onLogin

console.log('URL', page.url())
console.log('ON_LOGIN', onLogin)
console.log('ON_NOT_FOUND', onNotFound)
console.log('HAS_APP_UI', hasAppUi)
console.log('BODY', bodyText.slice(0, 800))
console.log('FAILED', JSON.stringify(failedRequests.slice(0, 30), null, 2))
console.log('ERRORS', JSON.stringify(errors.slice(0, 20), null, 2))

await page.screenshot({ path: '/tmp/streampark-app-page.png', fullPage: true })
await browser.close()
