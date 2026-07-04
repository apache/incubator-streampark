/**
 * Full audit: login, fetch backend menu, visit all routes, report issues.
 */
import { chromium } from 'playwright'
import qs from 'qs'

const API = process.argv[2] || 'http://127.0.0.1:10000'
const WEB = process.argv[3] || 'http://localhost:10002'

async function login() {
  const res = await fetch(`${API}/passport/signin`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: qs.stringify({ username: 'admin', password: 'streampark', loginType: 'PASSWORD' }),
  })
  const json = await res.json()
  const token = json.data?.token ?? json.token
  if (!token)
    throw new Error(`login failed: ${JSON.stringify(json).slice(0, 200)}`)
  return token
}

function collectPaths(routes, out = []) {
  for (const r of routes || []) {
    const p = r.path?.startsWith('/') ? r.path : `/${r.path || ''}`
    if (p && p !== '/')
      out.push({ path: p, title: r.meta?.title, hidden: r.meta?.hidden, component: r.component })
    if (r.children?.length)
      collectPaths(r.children, out)
  }
  return out
}

const token = await login()
const menuRes = await fetch(`${API}/menu/router`, {
  method: 'POST',
  headers: {
    Authorization: token,
    'Team-Id': '100000',
    'Content-Type': 'application/x-www-form-urlencoded',
  },
  body: '',
})
const menuJson = await menuRes.json()
const menuData = menuJson.data ?? menuJson
const allPaths = collectPaths(Array.isArray(menuData) ? menuData : menuData?.children ?? menuData)
console.log(`Backend menu paths: ${allPaths.length}`)

const browser = await chromium.launch()
const page = await browser.newPage()
const consoleErrors = []
const pageErrors = []

page.on('console', (msg) => {
  if (msg.type() === 'error')
    consoleErrors.push(msg.text())
})
page.on('pageerror', (err) => {
  pageErrors.push(String(err))
})

await page.goto(`${WEB}/#/login`, { waitUntil: 'domcontentloaded' })
await page.locator('input').first().fill('admin')
await page.locator('input[type="password"], input').nth(1).fill('streampark')
await page.getByRole('button', { name: /sign in|登录/i }).click()
await page.waitForURL(/#\/(flink|spark|system|setting|resource|project)/, { timeout: 15000 }).catch(() => {})
await page.waitForTimeout(3000)

const issues = []

async function auditPath(path, title) {
  const start = Date.now()
  const errorsBefore = consoleErrors.length
  await page.goto(`${WEB}/#${path}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1500)

  const bodyText = await page.locator('body').innerText()
  const is404 = /返回首页|Back Home|404|not found/i.test(bodyText) && bodyText.length < 800
  const hasTable = await page.locator('.n-data-table, .n-table, table').count()
  const hasCard = await page.locator('.n-card').count()
  const hasForm = await page.locator('.n-form, form').count()
  const hasEmpty = /暂无数据|No Data|empty/i.test(bodyText)
  const routeName = await page.evaluate(() => window.location.hash)
  const newErrors = consoleErrors.slice(errorsBefore).filter(e =>
    !e.includes('intlify') && !e.includes('ResizeObserver'),
  )

  const problem = []
  if (is404)
    problem.push('404')
  if (!is404 && hasTable === 0 && hasCard === 0 && hasForm === 0 && bodyText.trim().length < 200)
    problem.push('empty/minimal content')
  if (newErrors.length)
    problem.push(`console:${newErrors.slice(0, 2).join(' | ')}`)

  const elapsed = Date.now() - start
  const status = problem.length ? 'FAIL' : 'OK'
  console.log(`${status} ${path.padEnd(35)} ${elapsed}ms  table:${hasTable} card:${hasCard}  ${title || ''}  ${problem.join('; ')}`)
  if (problem.length)
    issues.push({ path, title, problem, sample: bodyText.slice(0, 150).replace(/\s+/g, ' ') })
}

for (const { path, title, hidden } of allPaths) {
  if (hidden)
    continue
  await auditPath(path, title)
}

// hidden but important routes
for (const path of [
  '/flink/app/add',
  '/flink/app/edit_streampark',
  '/flink/app/edit_flink',
  '/flink/app/detail',
  '/flink/cluster/add',
  '/project/add',
  '/spark/app/add',
  '/spark/app/edit',
  '/spark/app/detail',
]) {
  await auditPath(path, 'hidden')
}

console.log('\n=== SUMMARY ===')
console.log(`Issues: ${issues.length}`)
for (const i of issues)
  console.log(JSON.stringify(i))

if (pageErrors.length) {
  console.log('\nPage errors:')
  pageErrors.slice(0, 10).forEach(e => console.log(e))
}

await browser.close()
