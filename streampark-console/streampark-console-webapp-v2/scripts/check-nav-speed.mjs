import { chromium } from 'playwright'

const base = process.argv[2] || 'http://localhost:10002'
const browser = await chromium.launch()
const page = await browser.newPage()

await page.goto(`${base}/#/login`, { waitUntil: 'networkidle' })
await page.locator('input').first().fill('admin')
await page.locator('input[type="password"]').fill('streampark')
await page.getByRole('button', { name: /sign in|登录/i }).click()
await page.waitForTimeout(6000)

const pages = ['/flink/app', '/flink/home', '/flink/cluster', '/system/user', '/flink/app']
const timings = []

for (const path of pages) {
  const start = Date.now()
  await page.goto(`${base}/#${path}`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.n-data-table, .n-card, .n-form', { timeout: 8000 }).catch(() => {})
  const ms = Date.now() - start
  const body = (await page.locator('body').innerText()).slice(0, 100).replace(/\s+/g, ' ')
  const is404 = /返回首页|Back Home/.test(body)
  timings.push({ path, ms, is404, body: body.slice(0, 60) })
}

console.log(JSON.stringify(timings, null, 2))
await browser.close()
