import { spawn, spawnSync } from 'node:child_process';
import { readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

// Запускает собранное приложение (vite preview) и все tests/*.test.mjs по очереди, каждый в своём процессе.
const dir = dirname(fileURLToPath(import.meta.url));
const url = process.env.TEST_URL ?? 'http://localhost:4183';

const server = spawn('npx', ['vite', 'preview', '--port', '4183', '--strictPort'], { stdio: 'ignore', cwd: join(dir, '..') });

async function ready() {
  for (let i = 0; i < 60; i++) {
    try {
      if ((await fetch(url)).ok) return true;
    } catch {
      /* ещё не поднялся */
    }
    await new Promise((r) => setTimeout(r, 500));
  }
  return false;
}

let failed = 0;
if (!(await ready())) {
  console.error('Сервер не запустился');
  failed = 1;
} else {
  for (const file of readdirSync(dir).filter((f) => f.endsWith('.test.mjs')).sort()) {
    const r = spawnSync('node', [join(dir, file)], { stdio: 'inherit', timeout: 120000 });
    if (r.status !== 0) {
      failed++;
      console.error(`FAIL - ${file}`);
    }
  }
}
server.kill();
console.log(failed ? `Провалено тестов: ${failed}` : 'Все тесты прошли');
process.exit(failed ? 1 : 0);
