import assert from 'node:assert/strict';
import { open, pass } from './helpers.mjs';

// Усиление громкости выбирается и запоминается; у «Энергосбережения» появляется строка состояния.
const app = await open();
await app.tab('Звук');
await app.page.getByRole('button', { name: '200%' }).click();
await app.page.waitForTimeout(500); // запись в базу идёт пачкой через 0,25 с
assert.equal(await app.page.evaluate(() => localStorage.getItem('audioshelf.boost')), '200');
await app.page.reload();
await app.page.waitForTimeout(800);
await app.tab('Звук');
assert.equal(await app.page.getByRole('button', { name: '200%' }).getAttribute('aria-pressed'), 'true');

await app.tab('Настройки');
const energy = app.page.locator('.energy');
assert.equal(await energy.locator('.energy-status').isVisible(), false, 'пока выключено, строки состояния нет');
await energy.getByRole('button', { name: 'Вкл' }).click();
await app.page.waitForTimeout(700);
assert.match(await energy.locator('.energy-status').innerText(), /Включено/);
await app.done();
pass('boost-energy');
