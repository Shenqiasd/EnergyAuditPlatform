import { expect, test } from '@playwright/test'

test('front end serves the login experience', async ({ page }) => {
  await page.goto('/')

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('欢迎登录')).toBeVisible()
  await expect(page.getByText('企业端')).toBeVisible()
  await expect(page.getByText('管理端')).toBeVisible()
  await expect(page.getByText('审核端')).toBeVisible()
})

test('unauthenticated enterprise route redirects to login with redirect query', async ({ page }) => {
  await page.goto('/enterprise/dashboard')

  await expect(page).toHaveURL(/\/login\?redirect=.*enterprise.*dashboard/)
  await expect(page.getByPlaceholder('统一社会信用代码或用户名')).toBeVisible()
})
