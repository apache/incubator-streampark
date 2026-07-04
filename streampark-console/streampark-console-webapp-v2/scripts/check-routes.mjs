import { chromium } from 'playwright'

const base = process.argv[2] || 'http://localhost:10002'
const browser = await chromium.launch()
const page = await browser.newPage()

await page.goto(`${base}/#/login`, { waitUntil: 'networkidle' })
await page.locator('input').first().fill('admin')
await page.locator('input[type="password"], input').nth(1).fill('streampark')
await page.getByRole('button', { name: /sign in|登录/i }).click()
await page.waitForTimeout(7000)

const routes = await page.evaluate(() => {
  // @ts-ignore
  const r = window.__VUE_ROUTER__ || null
  return null
})

// use app internal - navigate and check matched
for (const path of ['/flink/app/add', '/flink/app/edit_streampark?appId=100000', '/flink/app/detail?appId=100000', '/project/add', '/spark/app/add']) {
  await page.goto(`${base}/#${path}`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(2000)
  const text = (await page.locator('body').innerText()).slice(0, 120).replace(/\s+/g, ' ')
  const is404 = text.includes('返回首页') || text.includes('Back Home')
  console.log(path, '404?', is404, 'sample:', text)
}

await browser.close()
