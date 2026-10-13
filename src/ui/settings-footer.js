import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';

// Низ настроек: версия (из package.json) и копирайт.
export function createSettingsFooter() {
  const box = el('footer', 'settings-footer');
  box.append(
    bindText(el('div', 'footer-version'), 'settings.version', { v: __APP_VERSION__ }),
    el('div', 'footer-copy', `© ${new Date().getFullYear()} DataLaw`),
  );
  return box;
}
