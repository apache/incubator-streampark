import { chromium } from 'playwright'

const base = process.argv[2] || 'http://localhost:10002'
const browser = await chromium.launch()
const page = await browser.newPage()
const errors = []
page.on('pageerror', e => errors.push(e.message))

async function login() {
  await page.goto(`${base}/#/login`, { waitUntil: 'networkidle', timeout: 60000 })
  await page.locator('input').first().fill('admin')
  await page.locator('input[type="password"], input').nth(1).fill('streampark')
  await page.getByRole('button', { name: /sign in|登录/i }).click()
  await page.waitForTimeout(6000)
}

await login()

const routes = [
  ['/flink/app/add', /添加|取消|Add|Cancel/],
  ['/flink/add_cluster', /集群|Cluster|取消/],
  ['/resource/project/add', /项目|Project|取消/],
  ['/flink/app', null],
]

for (const [path, pattern] of routes) {
  await page.goto(`${base}/#${path}`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(2500)
  const text = await page.locator('body').innerText()
  const is404 = /返回首页|Back Home/.test(text)
  const ok = pattern ? pattern.test(text) : /作业管理|Applications/.test(text)
  console.log(path, '404', is404, 'contentOk', ok)
}

// Flink list: add button + row ops
await page.goto(`${base}/#/flink/app`, { waitUntil: 'networkidle' })
await page.waitForTimeout(3000)
await page.locator('#e2e-flinkapp-create-btn').click()
await page.waitForTimeout(1500)
console.log('CREATE_NAV', page.url(), !page.url().includes('not-found'))

await page.goto(`${base}/#/flink/app`, { waitUntil: 'networkidle' })
await page.waitForTimeout(3000)
const opBtn = page.locator('.n-data-table-td').last().locator('.n-button').first()
if (await opBtn.isVisible()) {
  await opBtn.click()
  await page.waitForTimeout(1500)
  console.log('EDIT_NAV', page.url().includes('edit_streampark'))
}

// Detail via job name click
await page.goto(`${base}/#/flink/app`, { waitUntil: 'networkidle' })
await page.waitForTimeout(3000)
const jobLink = page.getByText('Flink SQL Demo').first()
if (await jobLink.isVisible()) {
  await jobLink.click()
  await page.waitForTimeout(1500)
  console.log('DETAIL_NAV', page.url().includes('detail'))
}

console.log('ERRORS', errors)
await browser.close()
process.exit(errors.length ? 1 : 0)
