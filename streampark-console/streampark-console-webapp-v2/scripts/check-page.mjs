import { chromium } from 'playwright'

const url = process.argv[2] || 'http://localhost:10002/#/login'
const browser = await chromium.launch()
const page = await browser.newPage()
const errors = []
const warnings = []

page.on('pageerror', e => errors.push(`PAGE: ${e.message}`))
page.on('console', msg => {
  if (msg.type() === 'error')
    errors.push(`CONSOLE: ${msg.text()}`)
  if (msg.type() === 'warning')
    warnings.push(msg.text())
})

await page.goto(url, { waitUntil: 'networkidle', timeout: 60000 })
await page.waitForTimeout(3000)

const bodyText = await page.locator('body').innerText().catch(() => '')
const html = await page.content()
const hasLogin = /login|sign|password|username|登录|密码/i.test(bodyText)
const hasNaive = html.includes('n-button') || html.includes('n-input') || html.includes('n-config-provider')
const appEmpty = (await page.locator('#app').innerHTML()).trim().length < 50

console.log('URL', page.url())
console.log('BODY_LEN', bodyText.length)
console.log('BODY_PREVIEW', bodyText.slice(0, 300).replace(/\s+/g, ' '))
console.log('APP_EMPTY', appEmpty)
console.log('HAS_LOGIN_UI', hasLogin)
console.log('HAS_NAIVE', hasNaive)
console.log('ERRORS', JSON.stringify(errors.slice(0, 15), null, 2))
console.log('WARNINGS', JSON.stringify(warnings.slice(0, 5), null, 2))

await page.screenshot({ path: '/tmp/streampark-webapp-check.png', fullPage: true })
console.log('SCREENSHOT', '/tmp/streampark-webapp-check.png')

await browser.close()
process.exit(errors.length > 0 || appEmpty ? 1 : 0)
