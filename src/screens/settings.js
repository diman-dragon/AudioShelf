import { el } from '../core/dom.js';
import { createArt } from '../ui/art.js';
import { createLangSwitch } from '../ui/lang-switch.js';
import { createRow } from '../ui/settings-row.js';
import { createSectionTitle } from '../ui/section-title.js';
import { createSettingsFooter } from '../ui/settings-footer.js';
import { createThemeSwitch } from '../ui/theme-switch.js';
import settingsArt from '../assets/art/settings.webp';

export function renderSettings() {
  const root = el('section', 'settings');
  root.append(
    createArt(settingsArt, 140, 'settings-art'),
    createSectionTitle('settings.theme'),
    createThemeSwitch(),
    createSectionTitle('settings.language'),
    createLangSwitch(),
    createSectionTitle('settings.playback'),
    createRow('⏪', 'settings.seekBack', 'settings.seconds', { n: 15 }),
    createRow('⏩', 'settings.seekForward', 'settings.seconds', { n: 30 }),
    createSettingsFooter(),
  );
  return root;
}
