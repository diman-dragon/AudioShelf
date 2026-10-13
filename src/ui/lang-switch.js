import { LANGS } from '../i18n/languages.js';
import { getLang } from '../i18n/i18n.js';
import { changeLanguage } from '../i18n/language.js';
import { createChoiceSwitch } from './choice-switch.js';

const items = LANGS.map((l) => ({ id: l.id, icon: l.badge, label: l.label }));

export const createLangSwitch = () => createChoiceSwitch(items, getLang, changeLanguage);
