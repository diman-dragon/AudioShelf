import assert from 'node:assert/strict';
import { open, pass } from './helpers.mjs';

const free = await open();
await free.tab('Настройки');
const tabs = await free.page.locator('.stabs button').allInnerTexts();
assert.deepEqual(tabs, ['Настройки приложения', 'О нас', 'Премиум']);
await free.page.locator('.stabs button').nth(2).click();
assert.match(await free.page.locator('.seal').innerText(), /не активирован/);
await free.done();

const owner = await open({ storage: { 'audioshelf.devcode': 'TEST' } });
await owner.tab('Настройки');
await owner.page.locator('.stabs button').nth(2).click();
assert.match(await owner.page.locator('.seal').innerText(), /Премиум получен/);
assert.equal(await owner.page.locator('.seal-ring').evaluate((e) => getComputedStyle(e).animationName), 'none');
await owner.done();
pass('settings-pages');
