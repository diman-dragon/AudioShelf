import { App } from '@capacitor/app';
import { HOME, getScreen, goHome } from './router.js';

// Системная «Назад»: с любого экрана — в библиотеку.
// В библиотеке ничего не происходит, приложение остаётся открытым.
export function initBack() {
  App.addListener('backButton', () => {
    if (getScreen() !== HOME) goHome();
  });
}
