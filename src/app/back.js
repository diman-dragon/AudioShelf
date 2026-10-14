import { App } from '@capacitor/app';
import { closeModal, isModalOpen } from '../ui/modal.js';
import { closeSheet, isSheetOpen } from '../ui/sheet.js';
import { closePage, getPage } from '../settings/settings-nav.js';
import { HOME, getScreen, goHome } from './router.js';

// Системная «Назад»: сначала закрывает открытое окно или меню; из раздела настроек — к списку разделов; потом с любого экрана — в библиотеку.
// В библиотеке ничего не происходит, приложение остаётся открытым.
export function initBack() {
  App.addListener('backButton', () => {
    if (isModalOpen()) return closeModal();
    if (isSheetOpen()) return closeSheet();
    if (getScreen() === 'settings' && getPage()) return closePage(); // из раздела — к списку разделов
    if (getScreen() !== HOME) goHome();
  });
}
