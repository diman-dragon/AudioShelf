import assert from 'node:assert/strict';
import { open, pass, pickFolder } from './helpers.mjs';

const app = await open();
await pickFolder(app);
assert.equal(await app.page.locator('.fprog:visible').count(), 0, 'у неначатых книг полоски нет');
await app.page.locator('.item').first().click();
await app.page.waitForTimeout(3500);
await app.tab('Библиотека');
assert.equal(await app.page.locator('.fprog:visible').count(), 1);
assert.match(await app.page.locator('.fprog-pc:visible').innerText(), /^\d+%$/);
await app.page.reload();
await app.page.waitForTimeout(1500);
assert.equal(await app.page.locator('.fprog:visible').count(), 1, 'после перезапуска полоска на месте');
await app.done();
pass('library-progress');
