import { el } from '../core/dom.js';
import { createFolderRow } from '../ui/folder-row.js';
import { createHiddenRow } from '../ui/hidden-row.js';
import { createLangSwitch } from '../ui/lang-switch.js';
import { createRow } from '../ui/setting-row.js';
import { createSettingsFooter } from '../ui/settings-footer.js';
import { createThemeSwitch } from '../ui/theme-switch.js';

export function renderSettings() {
  const root = el('section', 'settings');
  root.append(
    createFolderRow(),
    createHiddenRow(),
    createRow('settings.theme', createThemeSwitch()),
    createRow('settings.language', createLangSwitch()),
    createSettingsFooter(),
  );
  return root;
}
