// 10 полос эквалайзера: центральные частоты (Гц) и подписи под ползунками.
const FREQS = [31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000];
export const LABELS = ['31', '62', '125', '250', '500', '1k', '2k', '4k', '8k', '16k'];
export const MIN_DB = -12;
export const MAX_DB = 12;
export const FLAT_BANDS = FREQS.map(() => 0);
