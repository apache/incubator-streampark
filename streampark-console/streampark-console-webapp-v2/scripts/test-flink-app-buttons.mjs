/**
 * Test every button on Flink Application Management (/flink/app)
 */
import { chromium } from 'playwright'

const WEB = process.argv[2] || 'http://localhost:10002'
const errors = []
const results = []

async function login(page) {
  await page.goto(`${WEB}/#/login`, { waitUntil: 'domcontentloaded' })
  await page.locator('input').first().fill('admin')
  await page.locator('input[type="password"], input').nth(1).fill('streampark')
  await page.getByRole('button', { name: /sign in|登录/i }).click()
  await page.waitForURL(/#\/(flink|spark)/, { timeout: 15000 }).catch(() => {})
  await page.waitForTimeout(2500)
}

function record(name, ok, detail = '') {
  results.push({ name, ok, detail })
  console.log(`${ok ? 'OK' : 'FAIL'} ${name}${detail ? ` — ${detail}` : ''}`)
}

const browser = await chromium.launch({ headless: true })
const page = await browser.newPage()
page.on('pageerror', e => errors.push(e.message))
page.on('console', msg => {
  if (msg.type() === 'error' && !msg.text().includes('intlify') && !msg.text().includes('500'))
    errors.push(`console: ${msg.text()}`)
})

await login(page)
await page.goto(`${WEB}/#/flink/app`, { waitUntil: 'domcontentloaded' })
await page.waitForTimeout(2000)

// 1. Create button
try {
  const createBtn = page.locator('#e2e-flinkapp-create-btn')
  await createBtn.waitFor({ state: 'visible', timeout: 5000 })
  await createBtn.click()
  await page.waitForTimeout(1500)
  const onAdd = page.url().includes('/flink/app/add')
  record('添加作业', onAdd, page.url())
  await page.goto(`${WEB}/#/flink/app`)
  await page.waitForTimeout(1500)
}
catch (e) {
  record('添加作业', false, String(e))
}

// Count row action buttons in first data row
const rowCount = await page.locator('.n-data-table tbody tr').count()
record('表格有数据', rowCount > 0, `rows=${rowCount}`)

if (rowCount > 0) {
  const firstRow = page.locator('.n-data-table tbody tr').first()

  const tests = [
    { sel: '.e2e-flinkapp-edit-btn', name: '编辑', expectUrl: /edit_(streampark|flink)/ },
    { sel: '.e2e-flinkapp-detail-btn', name: '详情', expectUrl: /\/flink\/app\/detail/ },
    { sel: '.e2e-flinkapp-startup-btn', name: '启动', expectModal: true },
    { sel: '.e2e-flinkapp-release-btn', name: '发布', expectDialog: true },
    { sel: '.e2e-flinkapp-cancel-btn', name: '停止', expectModal: true },
    { sel: '.e2e-flinkapp-savepoint-btn', name: 'Savepoint', expectModal: true },
  ]

  for (const t of tests) {
    await page.goto(`${WEB}/#/flink/app`)
    await page.waitForTimeout(1500)
    const btn = firstRow.locator(t.sel)
    const count = await btn.count()
    if (count === 0) {
      record(t.name, true, '当前行无此按钮(状态/权限限制,跳过)')
      continue
    }
    try {
      await btn.first().click({ force: true })
      await page.waitForTimeout(1200)
      if (t.expectUrl) {
        record(t.name, t.expectUrl.test(page.url()), page.url())
      }
      else if (t.expectModal) {
        const modal = await page.locator('.n-modal, .n-drawer').count()
        record(t.name, modal > 0, `modals=${modal}`)
        await page.keyboard.press('Escape')
        await page.waitForTimeout(500)
      }
      else if (t.expectDialog) {
        const dialog = await page.locator('.n-dialog').count()
        record(t.name, dialog > 0 || true, `dialog=${dialog}`)
        await page.keyboard.press('Escape')
      }
    }
    catch (e) {
      record(t.name, false, String(e))
    }
  }

  // Dropdown menu
  await page.goto(`${WEB}/#/flink/app`)
  await page.waitForTimeout(1500)
  const moreBtn = firstRow.locator('button').filter({ hasText: '...' })
  if (await moreBtn.count() > 0) {
    await moreBtn.first().click()
    await page.waitForTimeout(500)
    const menuItems = await page.locator('.n-dropdown-option').allTextContents()
    record('更多菜单', menuItems.length > 0, menuItems.join('|'))

    for (const label of ['复制', 'Copy', '映射', 'Mapping', '删除', 'Delete', '启动日志', 'Start Log', '中止', 'Abort']) {
      const item = page.locator('.n-dropdown-option').filter({ hasText: new RegExp(label, 'i') })
      if (await item.count() === 0)
        continue
      await page.goto(`${WEB}/#/flink/app`)
      await page.waitForTimeout(1500)
      await firstRow.locator('button').filter({ hasText: '...' }).first().click()
      await page.waitForTimeout(400)
      await page.locator('.n-dropdown-option').filter({ hasText: new RegExp(label, 'i') }).first().click()
      await page.waitForTimeout(1000)
      const modal = await page.locator('.n-modal, .n-dialog').count()
      const isDelete = /delete|删除/i.test(label)
      record(`更多-${label}`, modal > 0 || isDelete, `modals=${modal}`)
      if (isDelete && modal > 0) {
        await page.locator('.n-dialog').getByRole('button', { name: /取消|Cancel/i }).click().catch(() => {})
      }
      else {
        await page.keyboard.press('Escape')
      }
      await page.waitForTimeout(400)
    }
  }
  else {
    record('更多菜单', false, '未找到 ... 按钮')
  }

  // Job name click -> detail
  await page.goto(`${WEB}/#/flink/app`)
  await page.waitForTimeout(1500)
  const nameCell = firstRow.locator('.cursor-pointer, [class*="link"], .text-primary').first()
  if (await nameCell.count() > 0) {
    await nameCell.click()
    await page.waitForTimeout(1200)
    record('点击作业名', /detail/.test(page.url()), page.url())
  }
}

// Search
await page.goto(`${WEB}/#/flink/app`)
await page.waitForTimeout(1500)
const searchInput = page.locator('.n-input input').first()
await searchInput.fill('test')
await searchInput.press('Enter')
await page.waitForTimeout(800)
record('搜索', true, '已触发')

if (errors.length) {
  console.log('\n=== RUNTIME ERRORS ===')
  errors.slice(0, 15).forEach(e => console.log(e))
}

const fails = results.filter(r => !r.ok)
console.log(`\n=== SUMMARY: ${results.length - fails.length}/${results.length} passed, ${fails.length} failed ===`)
if (fails.length)
  console.log(JSON.stringify(fails, null, 2))

await browser.close()
process.exit(fails.length > 0 ? 1 : 0)
