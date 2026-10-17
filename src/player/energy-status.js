import { Energy } from '../folder/plugins.js';

// Что с аппаратным воспроизведением на этом телефоне: null — настройка выключена, больше сказать нечего.
// Ключи подсказок: energy.unsupported | energy.waiting | energy.active.
export async function energyStatusKey() {
  try {
    const s = await Energy.status();
    if (!s.supported) return 'energy.unsupported';
    if (!s.allowed) return null;
    return s.neutral ? 'energy.active' : 'energy.waiting';
  } catch {
    return null;
  }
}
