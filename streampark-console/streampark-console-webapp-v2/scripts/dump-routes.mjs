import { chromium } from 'playwright'

const WEB = 'http://localhost:10002'
const browser = await chromium.launch()
const page = await browser.newPage()
await page.goto(`${WEB}/#/login`)
await page.locator('input').first().fill('admin')
await page.locator('input[type="password"], input').nth(1).fill('streampark')
await page.getByRole('button', { name: /sign in|登录/i }).click()
await page.waitForTimeout(5000)

const routes = await page.evaluate(() => {
  // @ts-ignore
  const r = window.__VUE_ROUTER__ || document.querySelector('#app')?.__vue_app__?.config?.globalProperties?.$router
  if (!r) return 'no router'
  return r.getRoutes().map(x => ({ path: x.path, name: x.name, redirect: x.redirect }))
})

console.log(JSON.stringify(routes, null, 2))
await browser.close()
