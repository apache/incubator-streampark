import { chromium } from 'playwright'

const base = process.argv[2] || 'http://localhost:10002'
const browser = await chromium.launch()
const page = await browser.newPage()
const errors = []
const logs = []

page.on('pageerror', e => errors.push(`PAGE: ${e.message}`))
page.on('console', msg => {
  if (msg.type() === 'error')
    errors.push(`CONSOLE: ${msg.text()}`)
  logs.push(`[${msg.type()}] ${msg.text()}`)
})

await page.goto(`${base}/#/login`, { waitUntil: 'networkidle', timeout: 60000 })
await page.locator('input').first().fill('admin')
await page.locator('input[type="password"], input').nth(1).fill('streampark')
await page.getByRole('button', { name: /sign in|登录/i }).click()
await page.waitForTimeout(6000)

// Test Add button
const addBtn = page.locator('#e2e-flinkapp-create-btn, button:has-text("添加"), button:has-text("Add")').first()
const addVisible = await addBtn.isVisible().catch(() => false)
console.log('ADD_BTN_VISIBLE', addVisible)
if (addVisible) {
  const urlBefore = page.url()
  await addBtn.click()
  await page.waitForTimeout(2000)
  console.log('URL_AFTER_ADD_CLICK', page.url(), 'changed', page.url() !== urlBefore)
}

// Test row action edit button if exists
const editBtn = page.locator('.e2e-flinkapp-edit-btn, button[class*="e2e-flinkapp-edit"]').first()
const editVisible = await editBtn.isVisible().catch(() => false)
console.log('EDIT_BTN_VISIBLE', editVisible)
if (editVisible) {
  const urlBefore = page.url()
  await editBtn.click()
  await page.waitForTimeout(2000)
  console.log('URL_AFTER_EDIT_CLICK', page.url(), 'changed', page.url() !== urlBefore)
}

// Test sidebar menu click
const homeMenu = page.getByText(/Flink 版本|flink\.flinkHome/i).first()
if (await homeMenu.isVisible().catch(() => false)) {
  const urlBefore = page.url()
  await homeMenu.click()
  await page.waitForTimeout(2000)
  console.log('URL_AFTER_HOME_MENU', page.url(), 'changed', page.url() !== urlBefore)
}

console.log('ERRORS', JSON.stringify(errors.slice(0, 15), null, 2))
await page.screenshot({ path: '/tmp/streampark-buttons.png', fullPage: true })
await browser.close()
