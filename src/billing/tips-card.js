import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { t } from '../i18n/i18n.js';
import { buy, priceOf } from './entitlement.js';
import { TIPS } from './products.js';

export function createTipsCard() {
  const node = el('div', 'tips');
  const row = el('div', 'tips-row');
  const status = el('p', 'note');
  status.hidden = true;
  const buttons = TIPS.map((tip) => {
    const btn = button('btn ghost tip', async () => {
      buttons.forEach((b) => (b.btn.disabled = true));
      const result = await buy(tip.id);
      buttons.forEach((b) => (b.btn.disabled = false));
      status.hidden = result === 'cancelled';
      status.textContent = result === 'cancelled' ? '' : t(result === 'ok' ? 'tip.thanks' : `buy.${result}`);
    });
    row.append(btn);
    return { btn, tip };
  });
  node.append(bindText(el('h2', 'sound-h'), 'tip.title'), bindText(el('p', 'note'), 'tip.hint'), row, status);

  const paint = () =>
    buttons.forEach(({ btn, tip }) => {
      btn.replaceChildren(el('span', 'tip-name', t(tip.nameKey)), el('span', 'tip-price', priceOf(tip.id)));
    });
  ['premiumchange', 'langchange'].forEach((name) => document.addEventListener(name, paint));
  paint();
  return node;
}
