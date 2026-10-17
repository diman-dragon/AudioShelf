import { Capacitor, registerPlugin } from '@capacitor/core';

// Нативная база данных приложения (android/.../AppDb.java). В браузере её нет: там записи лежат в localStorage.
export const Db = registerPlugin('Db');
export const isNative = Capacitor.isNativePlatform();
