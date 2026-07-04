import { chromium } from 'playwright'

const base = process.argv[2] || 'http://localhost:10002'
const browser = await chromium.launch()
const page = await browser.newPage()
const logs = []
page.on('console', msg => logs.push(`[${msg.type()}] ${msg.text()}`))

await page.goto(`${base}/#/login`, { waitUntil: 'networkidle' })
await page.locator('input').first().fill('admin')
await page.locator('input[type="password"], input').nth(1).fill('streampark')
await page.getByRole('button', { name: /sign in|登录/i }).click()
await page.waitForTimeout(7000)

await page.goto(`${base}/#/flink/app/add`, { waitUntil: 'networkidle' })
await page.waitForTimeout(2000)

console.log('ROUTER_LOGS', logs.filter(l => /router|view not found|warn/i.test(l)).join('\n'))
console.log('BODY', (await page.locator('body').innerText()).slice(0, 200))

await browser.close()
