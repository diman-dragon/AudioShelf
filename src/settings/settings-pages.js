import { createFolderRow } from '../ui/folder-row.js';
import { createHiddenRow } from '../ui/hidden-row.js';
import { createLangSwitch } from '../ui/lang-switch.js';
import { createRow } from '../ui/setting-row.js';
import { createSkipSwitch } from '../ui/skip-switch.js';
import { createSpeedSwitch } from '../ui/speed-switch.js';
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
      createRow('settings.speed', createSpeedSwitch(), true),
    ],
  },
  {
    id: 'interface',
    titleKey: 'settings.sectionInterface',
    build: () => [createRow('settings.theme', createThemeSwitch()), createRow('settings.language', createLangSwitch())],
  },
];
