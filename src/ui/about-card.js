import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';

const LOGO = `<svg viewBox="0 0 48 48" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
<path d="M9 40h30"/><rect x="11" y="13" width="7" height="27" rx="1.5"/><rect x="20.5" y="8" width="7" height="32" rx="1.5"/>
<path d="M30 40L36.5 12.5l6.2 1.5L36 41.5"/><path d="M24 17v14" stroke-width="1.6"/></svg>`;

export function createAboutCard() {
  const frame = el('div', 'about');
  const inner = el('div', 'about-in');
  ['tl', 'tr', 'bl', 'br'].forEach((c) => frame.append(el('span', `about-corner ${c}`)));
  const logo = el('div', 'about-logo');
  logo.innerHTML = LOGO;
  const version = bindText(el('div', 'about-ver'), 'settings.version', { v: __APP_VERSION__ });
  inner.append(
    logo,
    el('h2', 'about-name', 'AudioShelf'),
    bindText(el('div', 'about-tag'), 'about.tag'),
    version,
    el('div', 'about-rule'),
    bindText(el('p', 'about-text'), 'about.lead'),
    bindText(el('p', 'about-text'), 'about.maker'),
    el('div', 'about-copy', `© ${new Date().getFullYear()} DataLaw`),
  );
  frame.append(inner);
  return frame;
}
