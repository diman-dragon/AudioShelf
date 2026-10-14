// Пиктограммы режимов (currentColor).
const open = '<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"';

const repeat = '<path d="M17 3l3 3-3 3"/><path d="M4 11V9a3 3 0 0 1 3-3h13"/><path d="M7 21l-3-3 3-3"/><path d="M20 13v2a3 3 0 0 1-3 3H4"/>';

export const MODE_ICONS = {
  continue: `${open}><path d="M4 12h13"/><path d="M13 7l5 5-5 5"/></svg>`,
  chapter: `${open}><path d="M4 12h9"/><path d="M9 8l4 4-4 4"/><path d="M19 5v14"/></svg>`,
  off: `${open}><path d="M3 8h10M3 12h10M3 16h6"/><path d="M17 9l4 3-4 3z" fill="currentColor"/></svg>`,
  all: `${open}>${repeat}</svg>`,
  one: `${open}>${repeat}<text x="12" y="15" text-anchor="middle" font-size="8" font-weight="700" stroke="none" fill="currentColor">1</text></svg>`,
  shuffle: `${open}><path d="M3 7h3.5c5 0 6 10 11 10H21"/><path d="M3 17h3.5c1.6 0 2.7-.9 3.6-2.1"/><path d="M13.8 9.1C14.7 7.9 15.8 7 17.5 7H21"/><path d="M18 4l3 3-3 3"/><path d="M18 14l3 3-3 3"/></svg>`,
};

export const MARK_ICON = `${open}><path d="M6 4h12v17l-6-4.5L6 21z"/></svg>`;
