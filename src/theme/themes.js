export const THEMES = [
  { id: 'light', labelKey: 'theme.light', bg: '#E9DCBF', light: true },
  { id: 'dark', labelKey: 'theme.dark', bg: '#000000', light: false },
];

export const findTheme = (id) => THEMES.find((t) => t.id === id) ?? THEMES[1];

export const systemTheme = () => (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
