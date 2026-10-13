import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';

export const createSectionTitle = (key) => bindText(el('div', 'settings-title'), key);
