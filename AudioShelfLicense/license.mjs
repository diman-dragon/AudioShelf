// Выдача ключей разработчика AudioShelf.
//   node license.mjs                       — спросит отпечаток (можно несколько подряд)
//   node license.mjs ABCD-EFGH-JKLM-NPQR   — выдаст ключ сразу
//   node license.mjs ABCD-EFGH-JKLM-NPQR "Иван, Pixel 7"   — с заметкой «кому»
// Каждый выданный ключ дописывается в licenses.txt в той папке, откуда запущена команда.
import { appendFileSync, existsSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import { createHmac } from 'node:crypto';
import { dirname, join } from 'node:path';
import { createInterface } from 'node:readline/promises';
import { fileURLToPath } from 'node:url';

const ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
const FILE = join(process.cwd(), 'licenses.txt');
const NL = '\r\n';

const HEADER = [
  '=== AudioShelf: ключи разработчика ===',
  '',
  'КАК ВЫДАТЬ КЛЮЧ',
  '  1. Тестировщик открывает в приложении: Настройки -> «Премиум и поддержка» -> «Код разработчика»',
  '     и присылает вам «Отпечаток телефона» (вид: ABCD-EFGH-JKLM-NPQR).',
  '  2. Запустите выдачу одним из способов:',
  '       - двойной щелчок по файлу  Выдать_ключ.bat  (Windows);',
  '       - или в терминале, в папке с программой:   node license.mjs',
  '       - или сразу с отпечатком:                  node license.mjs ABCD-EFGH-JKLM-NPQR "Иван, Pixel 7"',
  '     Нужен Node.js (nodejs.org). Программа спросит отпечаток и «кому» (необязательно), покажет ключ',
  '     и допишет строку в конец этого файла. Файл создаётся в папке, откуда вы запустили программу.',
  '  3. Отправьте тестировщику ключ (вид: XXXXX-XXXXX-XXXXX-XXXXX). Он вставляет его в то же окно',
  '     приложения и нажимает «Применить ключ». Премиум включается навсегда, интернет не нужен.',
  '',
  'ВАЖНО',
  '  - Ключ подходит только к тому телефону, чей отпечаток вы вводили. Отпечатки пишутся только',
  '    буквами A-Z и цифрами 2-7; регистр и дефисы не важны.',
  '  - Ключи работают в AudioShelf 0.9.3 и новее. В первой сборке 0.9.2 был другой формат ключей:',
  '    такое приложение нужно обновить.',
  '  - После сброса телефона к заводским настройкам отпечаток меняется: нужен новый ключ.',
  '  - Файл secret.hex — секрет, по которому считаются ключи. Не публиковать и не пересылать.',
  '    Если его заменить, все выданные ключи перестанут работать.',
  '',
  'ВЫДАННЫЕ КЛЮЧИ  (дата | отпечаток | ключ | заметка)',
  '----------------------------------------------------------------------',
].join(NL) + NL;

function secret() {
  const path = join(dirname(fileURLToPath(import.meta.url)), 'secret.hex');
  if (!existsSync(path)) fail(`Не найден файл secret.hex рядом с программой (${path}).`);
  const hex = readFileSync(path, 'utf8').trim();
  if (!/^[0-9a-f]{64}$/i.test(hex)) fail('secret.hex повреждён: ожидается 64 символа 0-9, a-f.');
  return Buffer.from(hex, 'hex');
}

function fail(message) {
  console.error(message);
  process.exit(1);
}

// Отпечаток -> 'ABCD-EFGH-JKLM-NPQR', либо текст ошибки.
function normalize(input) {
  const text = String(input).toUpperCase().replace(/[\s-]/g, '');
  const bad = [...new Set(text.match(/[^A-Z2-7]/g) ?? [])];
  if (bad.length) return { error: `В отпечатке бывают только буквы A-Z и цифры 2-7. Лишнее: ${bad.join(' ')} (возможно, перепутана 0/O или 1/I).` };
  if (text.length !== 16) return { error: `Нужно 16 символов, получено ${text.length}.` };
  return { fp: text.match(/.{4}/g).join('-') };
}

// 100 бит HMAC-SHA256 от отпечатка: 20 знаков по 5 бит, группами по 5. Так же считает приложение.
function keyFor(fp, key) {
  const mac = createHmac('sha256', key).update('audioshelf-dev:2:' + fp).digest();
  let bits = 0, acc = 0, out = '';
  for (let i = 0; i < mac.length && out.length < 20; i++) {
    acc = (acc << 8) | mac[i];
    bits += 8;
    while (bits >= 5 && out.length < 20) {
      out += ALPHABET[(acc >> (bits - 5)) & 31];
      bits -= 5;
    }
    acc &= (1 << bits) - 1;
  }
  return out.match(/.{5}/g).join('-');
}

const stamp = () => {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
};

function record(fp, key, note) {
  const fresh = !existsSync(FILE) || statSync(FILE).size === 0;
  const seen = !fresh && readFileSync(FILE, 'utf8').includes(fp);
  const line = `${stamp()} | ${fp} | ${key} | ${[note, seen ? '(повторная выдача)' : ''].filter(Boolean).join(' ')}`.trimEnd();
  if (fresh) writeFileSync(FILE, '﻿' + HEADER + line + NL, 'utf8');
  else appendFileSync(FILE, line + NL, 'utf8');
  return seen;
}

function issue(raw, note, key) {
  const { fp, error } = normalize(raw);
  if (error) return console.error(error);
  const k = keyFor(fp, key);
  const seen = record(fp, k, note);
  console.log(`\nОтпечаток: ${fp}\nКлюч:      ${k}${seen ? '\n(этому телефону ключ уже выдавался — он тот же)' : ''}\nЗаписано в: ${FILE}\n`);
}

const key = secret();
const [, , argFp, ...rest] = process.argv;
if (argFp) {
  issue(argFp, rest.join(' ').trim(), key);
} else if (!process.stdin.isTTY) {
  fail('Использование: node license.mjs ОТПЕЧАТОК ["заметка"]');
} else {
  const rl = createInterface({ input: process.stdin, output: process.stdout });
  console.log('Выдача ключей AudioShelf. Пустая строка вместо отпечатка — выход.\n');
  try {
    for (;;) {
      const fp = (await rl.question('Отпечаток телефона: ')).trim();
      if (!fp) break;
      if (normalize(fp).error) {
        console.error(normalize(fp).error + '\n');
        continue;
      }
      issue(fp, (await rl.question('Кому / заметка (можно пропустить): ')).trim(), key);
    }
  } catch {
    // ввод закрыт (Ctrl+D / Ctrl+C) — просто выходим
  }
  rl.close();
}
