import { createSegmented } from '../ui/segmented.js';
import { CARDS_ICON, LIST_ICON } from './icons.js';
import { getView, setView } from './view-pref.js';

const items = [
  { id: 'list', icon: LIST_ICON, ariaKey: 'lib.list' },
  { id: 'cards', icon: CARDS_ICON, ariaKey: 'lib.cards' },
];

export const createViewSwitch = () => createSegmented(items, getView, setView);
