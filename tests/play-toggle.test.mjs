import assert from 'node:assert/strict';
import { mockBook, open, pass } from './helpers.mjs';

// Кнопка «пуск/пауза» не теряет нажатий: при подряд идущих тапах состояние чередуется.
const app = await open({ storage: { 'audioshelf.last2': JSON.stringify(mockBook(3)) } });
await app.tab('Плеер');
const label = () => app.page.locator('.pplay').getAttribute('aria-label');
const seen = [];
for (let i = 0; i < 6; i++) {
  await app.page.locator('.pplay').click();
  await app.page.waitForTimeout(350);
  seen.push(await label());
}
assert.deepEqual(seen, ['Пауза', 'Играть', 'Пауза', 'Играть', 'Пауза', 'Играть']);
await app.page.waitForTimeout(1500);
assert.equal(await label(), 'Играть', 'после паузы состояние не меняется само');
await app.done();
pass('play-toggle');
