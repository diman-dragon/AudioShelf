import { chromium } from 'playwright';

export const URL = process.env.TEST_URL ?? 'http://localhost:4183';

export const mockBook = (n, id = `bk${n}`) => ({
  id,
  name: 'Тест',
  folder: true,
  files: Array.from({ length: n }, (_, i) => ({ uri: `mock://t/${id}/${i}`, name: `Глава ${i + 1}.mp3`, size: 1e7, modified: 1, dir: 'Тест', dirUri: 'mock://dir/t' })),
});

export async function open({ storage = {}, wait = 800 } = {}) {
  const browser = await chromium.launch();
  const page = await (await browser.newContext({ locale: 'ru-RU', viewport: { width: 390, height: 800 } })).newPage();
  const errors = [];
  page.on('pageerror', (e) => errors.push(e.message));
  page.on('dialog', (d) => {
    errors.push(`dialog: ${d.message()}`);
    d.dismiss();
  });
  await page.goto(URL);
  await page.evaluate((s) => Object.entries(s).forEach(([k, v]) => localStorage.setItem(k, v)), storage);
  await page.reload();
  await page.waitForTimeout(wait);
  return {
    page,
    errors,
    tab: async (name) => {
      await page.locator('.tabbar').getByText(name, { exact: true }).click();
      await page.waitForTimeout(300);
    },
    async done() {
      await browser.close();
      if (errors.length) throw new Error(`Ошибки на странице: ${errors.join('; ')}`);
    },
  };
}

export async function pickFolder(app) {
  await app.tab('Настройки');
  await app.page.getByRole('button', { name: /Выбрать|Сменить|папк/i }).first().click();
  await app.page.waitForTimeout(1500);
  await app.tab('Библиотека');
}

export async function countWrites(page, fn) {
  await page.evaluate(() => {
    window.__w = {};
    const set = Storage.prototype.setItem;
    Storage.prototype.setItem = function (key, value) {
      const name = key.replace(/\u0001/g, '|').split('|').slice(0, 2).join('|');
      window.__w[name] = (window.__w[name] || 0) + 1;
      return set.call(this, key, value);
    };
  });
  await fn();
  return page.evaluate(() => window.__w);
}

export const pass = (name) => console.log(`ok - ${name}`);
