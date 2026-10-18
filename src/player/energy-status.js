import { Energy } from '../folder/plugins.js';

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
