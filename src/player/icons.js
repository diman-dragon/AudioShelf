// Пиктограммы плеера (currentColor).
const open = '<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"';

// Круговая стрелка с числом секунд внутри.
const skip = (arrow, seconds) =>
  `${open}>${arrow}<text x="12" y="15.5" text-anchor="middle" font-size="7.5" font-weight="700" stroke="none" fill="currentColor">${seconds}</text></svg>`;

export const backIcon = (seconds) => skip('<path d="M5 12a7 7 0 1 0 2.1-5"/><path d="M5 4v4h4"/>', seconds);
export const forwardIcon = (seconds) => skip('<path d="M19 12a7 7 0 1 1-2.1-5"/><path d="M19 4v4h-4"/>', seconds);
export const PLAY_ICON = '<svg viewBox="0 0 24 24" aria-hidden="true" fill="currentColor"><path d="M8 5.5v13a1 1 0 0 0 1.5.86l10.5-6.5a1 1 0 0 0 0-1.72L9.5 4.64A1 1 0 0 0 8 5.5z"/></svg>';
export const PAUSE_ICON = '<svg viewBox="0 0 24 24" aria-hidden="true" fill="currentColor"><rect x="6" y="5" width="4.5" height="14" rx="1.2"/><rect x="13.5" y="5" width="4.5" height="14" rx="1.2"/></svg>';
export const PREV_ICON = '<svg viewBox="0 0 24 24" aria-hidden="true" fill="currentColor"><rect x="5" y="5" width="2.6" height="14" rx="1"/><path d="M19 6.2v11.6a1 1 0 0 1-1.6.8l-8-5.8a1 1 0 0 1 0-1.6l8-5.8A1 1 0 0 1 19 6.2z"/></svg>';
export const NEXT_ICON = '<svg viewBox="0 0 24 24" aria-hidden="true" fill="currentColor"><rect x="16.4" y="5" width="2.6" height="14" rx="1"/><path d="M5 6.2v11.6a1 1 0 0 0 1.6.8l8-5.8a1 1 0 0 0 0-1.6l-8-5.8A1 1 0 0 0 5 6.2z"/></svg>';
