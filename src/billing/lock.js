import { el, button } from '../core/dom.js';
import { bindAria, bindText } from '../i18n/bind.js';
import { isPremium } from './entitlement.js';
import { openPremium } from './premium-dialog.js';

export function lockable(node) {
  const wrap = el('div', 'lockable');
  const veil = bindAria(button('lock-veil', openPremium), 'premium.unlock');
  wrap.append(node, veil);
  const paint = () => wrap.classList.toggle('locked', !isPremium());
  document.addEventListener('premiumchange', paint);
  paint();
  return wrap;
}

export function createLockBanner() {
  const node = el('div', 'lock-banner');
  node.append(bindText(el('span'), 'premium.locked'), bindText(button('btn', openPremium), 'premium.unlock'));
  const paint = () => (node.hidden = isPremium());
  document.addEventListener('premiumchange', paint);
  paint();
  return node;
}
