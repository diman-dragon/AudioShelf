import { el } from '../core/dom.js';
import { createRow } from '../ui/settings-row.js';
import { createArt } from '../ui/art.js';
import settingsArt from '../assets/art/settings.webp';

export function renderSettings() {
  const root = el('section', 'settings');
  root.append(
    createArt(settingsArt, 140, 'settings-art'),
    createRow('🎨', 'Тема', 'AMOLED'),
    createRow('⏪', 'Перемотка назад', '15 с'),
    createRow('⏩', 'Перемотка вперёд', '30 с'),
    createRow('💡', 'Версия', '0.1.0'),
  );
  return root;
}
