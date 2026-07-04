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

const opBtnCount = await page.locator('.n-data-table .n-button').count()
console.log('TABLE_BUTTONS', opBtnCount)

// click first quaternary icon button in table
const firstOp = page.locator('.n-data-table .n-data-table-td').last().locator('.n-button').first()
console.log('FIRST_OP_VISIBLE', await firstOp.isVisible().catch(() => false))
if (await firstOp.isVisible().catch(() => false)) {
  const urlBefore = page.url()
  await firstOp.click()
  await page.waitForTimeout(2000)
  console.log('AFTER_OP_CLICK', page.url(), urlBefore !== page.url())
}

// Settings button
await page.goto(`${base}/#/flink/app`, { waitUntil: 'networkidle' })
await page.waitForTimeout(3000)
const settingBtn = page.locator('[class*="setting"], .n-icon').filter({ has: page.locator('svg') }).first()
// try layout setting - look for gear icon area
const allButtons = await page.locator('button, .n-button').all()
console.log('TOTAL_CLICKABLE', allButtons.length)

// User center / avatar area
const userBtn = page.locator('.n-dropdown-trigger, [class*="user"]').first()
console.log('USER_TRIGGER', await userBtn.isVisible().catch(() => false))

// Test add page submit/back
await page.goto(`${base}/#/flink/app/add`, { waitUntil: 'networkidle' })
await page.waitForTimeout(3000)
console.log('ADD_PAGE_URL', page.url())
console.log('ADD_PAGE_TEXT', (await page.locator('body').innerText()).slice(0, 300).replace(/\s+/g, ' '))

const backOrCancel = page.getByRole('button', { name: /取消|返回|Cancel|Back/i }).first()
console.log('BACK_BTN', await backOrCancel.isVisible().catch(() => false))
if (await backOrCancel.isVisible().catch(() => false)) {
  await backOrCancel.click()
  await page.waitForTimeout(1500)
  console.log('AFTER_BACK', page.url())
}

// Collapse sidebar
await page.goto(`${base}/#/flink/app`, { waitUntil: 'networkidle' })
await page.waitForTimeout(2000)
const collapse = page.locator('.n-layout-sider, aside').first()
const beforeWidth = await collapse.boundingBox().catch(() => null)
const collapseBtn = page.locator('button').filter({ has: page.locator('svg') }).nth(2)
if (await collapseBtn.isVisible().catch(() => false)) {
  await collapseBtn.click()
  await page.waitForTimeout(500)
}
console.log('ERRORS', errors.slice(0, 10))
await browser.close()
