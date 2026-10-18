import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { t } from '../i18n/i18n.js';
import { buy, isPremium, priceOf } from './entitlement.js';
import { PREMIUM } from './products.js';

const FEATURES = ['premium.f1', 'premium.f2', 'premium.free'];

export function createPremiumCard(onDone, titled = true) {
  const node = el('div', 'premium');
  const list = el('ul', 'premium-list');
  FEATURES.forEach((key) => list.append(bindText(el('li'), key)));
  const status = el('p', 'note');
  const action = button('btn', async () => {
    action.disabled = true;
    const result = await buy(PREMIUM);
    action.disabled = false;
    if (result === 'pending' || result === 'error') status.textContent = t(`buy.${result}`);
    else if (isPremium()) onDone?.();
    paint();
  });
  if (titled) node.append(bindText(el('h2', 'sound-h'), 'premium.title'));
  node.append(bindText(el('p', 'note'), 'premium.lead'), list, status, action);

  function paint() {
    const owned = isPremium();
    action.hidden = owned;
    action.textContent = priceOf(PREMIUM) ? t('premium.buy', { p: priceOf(PREMIUM) }) : t('premium.buyNoPrice');
    if (owned) status.textContent = t('premium.active');
    status.hidden = !status.textContent;
  }
  ['premiumchange', 'langchange'].forEach((name) => document.addEventListener(name, paint));
  paint();
  return node;
}
