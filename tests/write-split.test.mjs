import assert from 'node:assert/strict';
import { countWrites, mockBook, open, pass } from './helpers.mjs';

const app = await open({ storage: { 'audioshelf.last2': JSON.stringify(mockBook(3)) } });
await app.tab('Плеер');
const writes = await countWrites(app.page, async () => {
  await app.page.locator('.pplay').click();
  await app.page.waitForTimeout(9000);
  await app.page.locator('.pplay').click();
  await app.page.waitForTimeout(800);
});
assert.ok((writes['row|pos'] ?? 0) <= 2, `позиция писалась ${writes['row|pos']} раз`);
assert.ok((writes['row|pinfo'] ?? 0) <= 2, `сведения писались ${writes['row|pinfo']} раз`);
assert.ok((writes['row|durs'] ?? 0) <= 2, `длины писались ${writes['row|durs']} раз`);
assert.equal(writes['audioshelf.last2'], undefined, 'книга целиком не перезаписывается');
assert.equal(writes['row|lib'], undefined, 'библиотека не перезаписывается');
await app.done();
pass('write-split');
