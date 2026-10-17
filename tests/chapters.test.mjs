import assert from 'node:assert/strict';
import { open, pass } from './helpers.mjs';

// Файл с главами внутри (заглушка mock://chaptered) раскладывается на главы: заголовок, список, «следующая».
const book = {
  id: 'mock://chaptered/book',
  name: 'Большая книга',
  folder: false,
  files: [{ uri: 'mock://chaptered/book', name: 'Большая книга.m4b', size: 5e8, modified: 1 }],
};
const app = await open({ storage: { 'audioshelf.last2': JSON.stringify(book) }, wait: 1500 });
await app.tab('Плеер');
assert.match(await app.page.locator('.player').innerText(), /Глава 1 из 4/);
await app.page.locator('.pextra').nth(2).click(); // «Список»
const list = await app.page.locator('.modal').innerText();
for (const title of ['Пролог', 'Глава 1. Начало', 'Глава 2', 'Эпилог']) assert.ok(list.includes(title), `в списке нет «${title}»`);
await app.page.getByRole('button', { name: 'Закрыть' }).click();
await app.page.getByRole('button', { name: 'Следующая глава' }).click();
await app.page.waitForTimeout(500);
assert.match(await app.page.locator('.player').innerText(), /Глава 2 из 4/);
await app.done();
pass('chapters');
