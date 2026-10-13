import { applyTranslations } from './apply.js';
import { detectLang } from './detect.js';
import { getLang, setCurrent } from './i18n.js';
import { loadLang, saveLang } from './lang-storage.js';

function activate(id) {
  setCurrent(id);
  document.documentElement.lang = getLang();
  applyTranslations();
}

// При старте: выбор пользователя, а если его нет — язык устройства.
export function initLanguage() {
  activate(loadLang() ?? detectLang());
}

export function changeLanguage(id) {
  saveLang(id);
  activate(id);
}
