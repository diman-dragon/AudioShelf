import { showMessage } from '../ui/message.js';

// Ошибка «пользователь закрыл окно» — не ошибка.
const isCancel = (e) => String(e?.message ?? e).includes('cancel');

export function fail(e) {
  if (isCancel(e)) return;
  showMessage(String(e?.message ?? e).includes('no_viewer') ? 'lib.noViewer' : 'lib.fail');
}
