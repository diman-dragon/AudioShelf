import { Capacitor, registerPlugin } from '@capacitor/core';

export const Db = registerPlugin('Db');
export const isNative = Capacitor.isNativePlatform();
