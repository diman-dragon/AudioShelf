import { t } from '../i18n/i18n.js';

// Ошибка «пользователь закрыл окно» — не ошибка.
const isCancel = (e) => String(e?.message ?? e).includes('cancel');

export function fail(e) {
  if (isCancel(e)) return;
  alert(t(String(e?.message ?? e).includes('no_viewer') ? 'lib.noViewer' : 'lib.fail'));
}
