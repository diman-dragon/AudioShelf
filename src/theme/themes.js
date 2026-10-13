// Список тем. bg — цвет фона (для системных панелей), light — светлая ли тема.
export const THEMES = [
  { id: 'amoled', labelKey: 'theme.amoled', emoji: '🌑', bg: '#000000', light: false },
  { id: 'brown', labelKey: 'theme.brown', emoji: '🍂', bg: '#e9d6b8', light: true },
];

export const DEFAULT_THEME = 'amoled';

export const findTheme = (id) => THEMES.find((t) => t.id === id) ?? THEMES[0];
