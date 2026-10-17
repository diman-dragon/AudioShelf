import assert from 'node:assert/strict';
import { mockBook, open, pass } from './helpers.mjs';

// Во время игры экран плеера перерисовывается целиком редко, а время идёт отдельным «тиком» раз в секунду.
const app = await open({ storage: { 'audioshelf.last2': JSON.stringify(mockBook(3)) } });
await app.tab('Плеер');
await app.page.evaluate(() => {
  window.__ev = { change: 0, tick: 0 };
  document.addEventListener('playerchange', () => window.__ev.change++);
  document.addEventListener('playertick', () => window.__ev.tick++);
});
await app.page.locator('.pplay').click();
await app.page.waitForTimeout(4200);
const ev = await app.page.evaluate(() => window.__ev);
assert.ok(ev.change <= 2, `полных перерисовок ${ev.change}`);
assert.ok(ev.tick >= 3, `тиков ${ev.tick}`);

// Пока открыта другая вкладка, плеер не рисуется, а при возвращении время уже верное.
const time = () => app.page.locator('.player').innerText();
const before = await time();
await app.tab('Библиотека');
await app.page.waitForTimeout(2500);
await app.tab('Плеер');
await app.page.waitForTimeout(300);
assert.notEqual(await time(), before, 'время на экране плеера обновилось после возвращения');
await app.done();
pass('player-tick');
