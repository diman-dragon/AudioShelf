import { createPremiumCard } from '../billing/premium-card.js';
import { createPremiumSeal } from '../billing/premium-seal.js';
import { createTipsCard } from '../billing/tips-card.js';
import { createAboutCard } from '../ui/about-card.js';
import { createDevCard } from '../billing/dev-card.js';
import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { createEnergySwitch } from '../ui/energy-switch.js';
import { createFolderRow } from '../ui/folder-row.js';
import { createHiddenRow } from '../ui/hidden-row.js';
import { createLangSwitch } from '../ui/lang-switch.js';
import { createRow } from '../ui/setting-row.js';
import { createSkipSwitch } from '../ui/skip-switch.js';
import { createThemeSwitch } from '../ui/theme-switch.js';

const heading = (key) => bindText(el('h2', 'sound-h set-h'), key);

export const PAGES = [
  {
    id: 'app',
    titleKey: 'settings.pageApp',
    build: () => [
      heading('settings.sectionLibrary'),
      createFolderRow(),
      createHiddenRow(),
      heading('settings.sectionPlayback'),
      createRow('settings.skipBack', createSkipSwitch('back'), true),
      createRow('settings.skipForward', createSkipSwitch('forward'), true),
      createRow('settings.energy', createEnergySwitch(), true),
      heading('settings.sectionInterface'),
      createRow('settings.theme', createThemeSwitch()),
      createRow('settings.language', createLangSwitch()),
    ],
  },
  {
    id: 'about',
    titleKey: 'settings.sectionAbout',
    build: () => [createAboutCard()],
  },
  {
    id: 'premium',
    titleKey: 'settings.pagePremium',
    build: () => [createPremiumSeal(), createPremiumCard(undefined, false), createTipsCard(), createDevCard()],
  },
];
