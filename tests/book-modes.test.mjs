import assert from 'node:assert/strict';
import { mockBook, open, pass } from './helpers.mjs';

const app = await open({ storage: { 'audioshelf.last2': JSON.stringify(mockBook(3)) } });
await app.tab('Плеер');
const mode = app.page.locator('.pextra').nth(3);
assert.match(await mode.innerText(), /Без паузы/);
await mode.click();
assert.match(await mode.innerText(), /С паузой/);
const order = (await app.page.locator('.pextra').allInnerTexts()).map((t) => t.split('\n').pop());
assert.deepEqual([order[0], order[1], order[2], order[4]], ['Скорость', 'Сон', 'Список', 'Закладки'], 'порядок значков: скорость, сон, список, режим, закладки');
await app.done();
pass('book-modes');
