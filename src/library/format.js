import { getLang, t } from '../i18n/i18n.js';

const MB = 1048576;
const GB = 1073741824;

const num = (n) => n.toLocaleString(getLang(), { maximumFractionDigits: 1 });

export function fmtSize(bytes) {
  if (bytes < MB) return `${Math.max(1, Math.round(bytes / 1024))} ${t('unit.kb')}`;
  if (bytes < GB) return `${num(bytes / MB)} ${t('unit.mb')}`;
  return `${num(bytes / GB)} ${t('unit.gb')}`;
}

export const fmtDate = (ms) => new Date(ms).toLocaleDateString(getLang());

// 'Книга.m4b' -> ['Книга', '.m4b']
export function splitName(name) {
  const dot = name.lastIndexOf('.');
  return dot > 0 ? [name.slice(0, dot), name.slice(dot)] : [name, ''];
}
