import { createPremiumCard } from '../billing/premium-card.js';
import { createTipsCard } from '../billing/tips-card.js';
import { createAboutCard } from '../ui/about-card.js';
import { createDevCard } from '../billing/dev-card.js';
import { createEnergySwitch } from '../ui/energy-switch.js';
import { createFolderRow } from '../ui/folder-row.js';
import { createHiddenRow } from '../ui/hidden-row.js';
import { createLangSwitch } from '../ui/lang-switch.js';
import { createRow } from '../ui/setting-row.js';
import { createSkipSwitch } from '../ui/skip-switch.js';
import { createThemeSwitch } from '../ui/theme-switch.js';

// Разделы настроек по смыслу. build() собирает содержимое страницы.
export const PAGES = [
  {
    id: 'library',
    titleKey: 'settings.sectionLibrary',
    build: () => [createFolderRow(), createHiddenRow()],
  },
  {
    id: 'playback',
    titleKey: 'settings.sectionPlayback',
    build: () => [
      createRow('settings.skipBack', createSkipSwitch('back'), true),
      createRow('settings.skipForward', createSkipSwitch('forward'), true),
      createRow('settings.energy', createEnergySwitch(), true),
    ],
  },
  {
    id: 'support',
    titleKey: 'settings.sectionSupport',
    build: () => [createPremiumCard(), createTipsCard(), createDevCard()],
  },
  {
    id: 'interface',
    titleKey: 'settings.sectionInterface',
    build: () => [createRow('settings.theme', createThemeSwitch()), createRow('settings.language', createLangSwitch())],
  },
  {
    id: 'about',
    titleKey: 'settings.sectionAbout',
    build: () => [createAboutCard()],
  },
];
