import { THEMES } from '../theme/themes.js';
import { getTheme, setTheme } from '../theme/theme.js';
import { createChoiceSwitch } from './choice-switch.js';

const items = THEMES.map((th) => ({ id: th.id, icon: th.emoji, labelKey: th.labelKey }));

export const createThemeSwitch = () => createChoiceSwitch(items, getTheme, setTheme);
