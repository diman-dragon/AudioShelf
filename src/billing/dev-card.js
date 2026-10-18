import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { t } from '../i18n/i18n.js';
import { applyDevCode, getFingerprint, hasDevCode, removeDevCode } from './entitlement.js';

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    const area = el('textarea');
    area.value = text;
    area.style.cssText = 'position:fixed;opacity:0';
    document.body.append(area);
    area.select();
    let ok = false;
    try {
      ok = document.execCommand('copy');
    } catch {
    }
    area.remove();
    return ok;
  }
}

export function createDevCard() {
  const node = el('div', 'devcode');
  const fp = el('div', 'dev-fp');
  const copy = button('btn ghost', async () => {
    if (await copyText(fp.textContent)) {
      copy.textContent = t('dev.copied');
      setTimeout(() => (copy.textContent = t('dev.copy')), 1500);
    }
  });
  const input = el('input', 'dev-input');
  Object.assign(input, { type: 'text', autocomplete: 'off', spellcheck: false, maxLength: 40 });
  input.setAttribute('autocapitalize', 'characters');
  const status = el('p', 'note');
  const apply = button('btn', async () => {
    if (!input.value.trim()) return;
    apply.disabled = true;
    const ok = await applyDevCode(input.value);
    apply.disabled = false;
    if (ok) input.value = '';
    else status.textContent = t('dev.bad');
    paint();
  });
  const remove = button('btn ghost', () => {
    removeDevCode();
    status.textContent = '';
    paint();
  });
  const entry = el('div', 'dev-entry');
  entry.append(input, apply);
  node.append(bindText(el('h2', 'sound-h'), 'dev.title'), bindText(el('p', 'note'), 'dev.hint'), bindText(el('p', 'note'), 'dev.fp'), fp, copy, entry, status, remove);

  function paint() {
    const owned = hasDevCode();
    entry.hidden = owned;
    remove.hidden = !owned;
    if (owned) status.textContent = t('dev.active');
    status.hidden = !status.textContent;
    input.placeholder = t('dev.placeholder');
    apply.textContent = t('dev.apply');
    remove.textContent = t('dev.remove');
    copy.textContent = t('dev.copy');
  }
  getFingerprint().then((v) => (fp.textContent = v));
  ['premiumchange', 'langchange'].forEach((name) => document.addEventListener(name, paint));
  paint();
  return node;
}
