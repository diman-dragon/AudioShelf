import { t } from '../i18n/i18n.js';

// Строка состояния нормализации: идёт / остановлена / готово / сколько файлов уже проанализировано.
export function statusText(state, pending, total) {
  if (state.status === 'running') return t('norm.running', { n: state.done, m: state.total });
  if (state.status === 'battery') return t('norm.battery', { n: state.done, m: state.total });
  if (state.status === 'cancelled') return t('norm.cancelled', { n: state.done, m: state.total });
  if (state.status === 'done' && state.total) return t('norm.done', { n: state.done - state.failed, f: state.failed });
  if (!total) return t('norm.empty');
  return pending ? t('norm.idle', { n: total - pending, m: total }) : t('norm.allDone');
}
