import { chromium } from 'playwright'

const base = process.argv[2] || 'http://localhost:10002'
const browser = await chromium.launch()
const page = await browser.newPage()
const logs = []

page.on('console', msg => logs.push(`[${msg.type()}] ${msg.text()}`))

await page.goto(`${base}/#/login`, { waitUntil: 'networkidle', timeout: 60000 })
await page.locator('input').first().fill('admin')
await page.locator('input[type="password"], input').nth(1).fill('streampark')
await page.getByRole('button', { name: /sign in|登录/i }).click()
await page.waitForTimeout(8000)

const routeInfo = await page.evaluate(() => {
  const app = document.querySelector('#app')
  return {
    url: location.href,
    appHtmlLen: app?.innerHTML?.length ?? 0,
    appText: app?.innerText?.slice(0, 500) ?? '',
  }
})

console.log('ROUTE_INFO', JSON.stringify(routeInfo, null, 2))
console.log('LOGS', logs.filter(l => /router|route|error|warn|view not found/i.test(l)).slice(0, 30).join('\n'))

await page.screenshot({ path: '/tmp/streampark-debug.png', fullPage: true })
await browser.close()
