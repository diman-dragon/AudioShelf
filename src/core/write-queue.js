import { Db, isNative } from './db.js';
import { browserApply } from './browser-db.js';

// Очередь записи: изменения копятся недолго и уходят в базу одной транзакцией.
// Одна и та же запись за это время пишется один раз (последнее значение); очистка хранилища вытесняет прежние записи в нём.
// Операции: ['p', хранилище, ключ, значение], ['d', хранилище, ключ], ['c', хранилище].
const DELAY_MS = 250;
const RETRY_MS = 1000;

const pending = new Map(); // ключ операции -> операция (порядок вставки сохраняется)
let timer = 0;
let busy = false;
let again = false;

const id = (s, k) => `${s}\u0001${k}`;

export function queueOp(op) {
  const [kind, s, k] = op;
  if (kind === 'c') {
    [...pending.keys()].filter((key) => key.startsWith(`${s}\u0001`)).forEach((key) => pending.delete(key));
    pending.set(`c\u0002${s}`, op);
  } else {
    pending.delete(id(s, k)); // новая запись встаёт в конец, после возможной очистки
    pending.set(id(s, k), op);
  }
  if (!timer && !busy) timer = setTimeout(flushQueue, DELAY_MS);
}

export async function flushQueue() {
  clearTimeout(timer);
  timer = 0;
  if (busy) {
    again = true; // сбросить сразу, как закончится идущая запись
    return;
  }
  if (!pending.size) return;
  const ops = [...pending.values()];
  pending.clear();
  if (!isNative) return browserApply(ops);
  busy = true;
  try {
    await Db.apply({ ops });
  } catch {
    ops.reverse().forEach((op) => {
      const key = op[0] === 'c' ? `c\u0002${op[1]}` : id(op[1], op[2]);
      if (!pending.has(key)) pending.set(key, op); // не записалось: повторим, если за это время не появилось новее
    });
    timer = setTimeout(flushQueue, RETRY_MS);
  }
  busy = false;
  if (again) {
    again = false;
    flushQueue();
  }
}
