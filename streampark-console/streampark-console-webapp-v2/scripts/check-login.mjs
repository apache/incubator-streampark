import { chromium } from 'playwright'

const base = process.argv[2] || 'http://localhost:10002'
const browser = await chromium.launch()
const page = await browser.newPage()
const errors = []

page.on('pageerror', e => errors.push(`PAGE: ${e.message}`))
page.on('console', msg => {
  if (msg.type() === 'error')
    errors.push(`CONSOLE: ${msg.text()}`)
})

await page.goto(`${base}/#/login`, { waitUntil: 'networkidle', timeout: 60000 })
await page.waitForTimeout(1000)

const userInput = page.locator('input').first()
const passInput = page.locator('input[type="password"], input').nth(1)
await userInput.fill('admin')
await passInput.fill('streampark')

const signBtn = page.getByRole('button', { name: /sign in|登录/i })
await signBtn.click()

await page.waitForTimeout(5000)

const bodyText = await page.locator('body').innerText().catch(() => '')
console.log('URL_AFTER_LOGIN', page.url())
console.log('BODY_LEN', bodyText.length)
console.log('BODY_PREVIEW', bodyText.slice(0, 400).replace(/\s+/g, ' '))
console.log('ERRORS', JSON.stringify(errors.slice(0, 20), null, 2))

await page.screenshot({ path: '/tmp/streampark-after-login.png', fullPage: true })
console.log('SCREENSHOT', '/tmp/streampark-after-login.png')

await browser.close()
process.exit(errors.length > 0 ? 1 : 0)
