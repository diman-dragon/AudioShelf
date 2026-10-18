import assert from 'node:assert/strict';
import { open, pass, pickFolder } from './helpers.mjs';

const app = await open({ storage: { 'audioshelf.devcode': 'TEST' } });
await pickFolder(app);
await app.page.waitForTimeout(12000);
const gains = await app.page.evaluate(() => Object.keys(localStorage).filter((k) => k.includes('audioshelf.gain')).length);
assert.ok(gains >= 3, `сохранено поправок: ${gains}`);
await app.done();
pass('auto-normalize');
