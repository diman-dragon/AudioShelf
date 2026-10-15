import { Billing } from '../folder/plugins.js';
import { readStored, writeStored } from '../core/storage.js';

// Куплен ли Премиум. Ответ Google Play запоминаем: без сети всё продолжает работать.
const KEY = 'audioshelf.premium';
let prices = {};

export const isPremium = () => readStored(KEY) === '1';
export const priceOf = (id) => prices[id] || '';

// canRevoke: только полный запрос покупок может снять Премиум (возврат денег); ответ на покупку — только включить.
function apply(state, canRevoke) {
  if (Array.isArray(state.products) && state.products.length) {
    prices = Object.fromEntries(state.products.map((p) => [p.id, p.price]));
  }
  if (state.premium) writeStored(KEY, '1');
  else if (canRevoke) writeStored(KEY, '0');
  document.dispatchEvent(new Event('premiumchange'));
}

async function refreshBilling() {
  try {
    apply(await Billing.getState(), true);
  } catch {
    /* Google Play недоступен — остаётся запомненное */
  }
}

// ok | cancelled | pending | error
export async function buy(id) {
  try {
    const result = await Billing.buy({ id });
    apply(result, false);
    return result.status;
  } catch {
    return 'error';
  }
}

// Проверяем покупки при запуске, при возврате в приложение и когда отложенный платёж прошёл.
export function initBilling() {
  refreshBilling();
  Billing.addListener('billing', (s) => apply(s, false)).catch(() => {});
  document.addEventListener('visibilitychange', () => !document.hidden && refreshBilling());
}
