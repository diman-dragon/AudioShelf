import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { isPremium } from './entitlement.js';

const STAR = '<path d="M12 3.2l2.6 5.4 5.9.8-4.3 4.1 1 5.9L12 16.6 6.8 19.4l1-5.9L3.5 9.4l5.9-.8z"/>';
const CHECK = '<path d="M7.5 12.5l3 3 6-7" fill="none" stroke-linecap="round" stroke-linejoin="round"/>';
const LOCK = '<rect x="6" y="11" width="12" height="9" rx="2"/><path d="M8.5 11V8.5a3.5 3.5 0 0 1 7 0V11" fill="none"/>';

// Значок Премиума: золотая печать с галочкой, когда он получен; серая с замком, когда нет.
export function createPremiumSeal() {
  const node = el('div', 'seal');
  const ring = el('div', 'seal-ring');
  const icon = el('div', 'seal-icon');
  ring.append(icon);
  const label = el('div', 'seal-label');
  node.append(ring, label);
  const paint = () => {
    const owned = isPremium();
    node.classList.toggle('owned', owned);
    icon.innerHTML = `<svg viewBox="0 0 24 24" aria-hidden="true" fill="currentColor" stroke="currentColor" stroke-width="1.8">${owned ? STAR + CHECK.replace('fill="none"', 'fill="none" stroke="var(--seal-check)"') : LOCK}</svg>`;
    bindText(label, owned ? 'premium.sealOn' : 'premium.sealOff');
  };
  document.addEventListener('premiumchange', paint);
  paint();
  return node;
}
