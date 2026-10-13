import { t } from '../i18n/i18n.js';

// Путь к папке в понятном виде: «Память устройства › Audiobooks › Books».
// Понимает папки во внутренней памяти и на карте; для остальных (Загрузки, облако) берёт имя папки.
export function folderPath(folder) {
  const match = /externalstorage\.documents\/tree\/([^/?#]+)/.exec(folder?.uri ?? '');
  if (!match) return folder?.name ?? '';
  const [volume, rest = ''] = decodeURIComponent(match[1]).split(':');
  const root = volume === 'primary' ? t('path.internal') : t('path.card');
  return [root, ...rest.split('/').filter(Boolean)].join(' › ');
}
