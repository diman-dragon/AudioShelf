import { Billing, License } from '../folder/plugins.js';
import { readStored, removeStored, writeStored } from '../core/storage.js';

const KEY = 'audioshelf.premium';
const DEV_KEY = 'audioshelf.devcode';
let prices = {};
let devOk = false;

export const isPremium = () => devOk || readStored(KEY) === '1';
export const hasDevCode = () => devOk;
export const priceOf = (id) => prices[id] || '';

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
  }
}

export async function buy(id) {
  try {
    const result = await Billing.buy({ id });
    apply(result, false);
    return result.status;
  } catch {
    return 'error';
  }
}

export function initBilling() {
  refreshBilling();
  Billing.addListener('billing', (s) => apply(s, false)).catch(() => {});
  document.addEventListener('visibilitychange', () => !document.hidden && refreshBilling());
}

let fingerprint = '';

export const getFingerprint = async () => {
  if (!fingerprint) fingerprint = (await License.check({ code: '' }).catch(() => ({}))).fingerprint || '';
  return fingerprint;
};

function setDev(ok) {
  if (ok === devOk) return;
  devOk = ok;
  document.dispatchEvent(new Event('premiumchange'));
}

export async function initDevCode() {
  const code = readStored(DEV_KEY);
  if (code) setDev(Boolean((await License.check({ code }).catch(() => ({}))).valid));
}

export async function applyDevCode(code) {
  const res = await License.check({ code: code.trim() }).catch(() => ({}));
  if (res.fingerprint) fingerprint = res.fingerprint;
  if (!res.valid) return false;
  writeStored(DEV_KEY, code.trim());
  setDev(true);
  return true;
}

export function removeDevCode() {
  removeStored(DEV_KEY);
  setDev(false);
}
