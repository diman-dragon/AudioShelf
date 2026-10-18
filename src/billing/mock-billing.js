import { readStored, writeStored } from '../core/storage.js';

const OWNED = 'audioshelf.mockPremium';
const PRICES = [
  { id: 'premium', price: '$9.99' },
  { id: 'tip_small', price: '$0.99' },
  { id: 'tip_medium', price: '$2.99' },
  { id: 'tip_large', price: '$4.99' },
];
const state = () => ({ premium: readStored(OWNED) === '1', products: PRICES });

export const mockBilling = {
  async getState() {
    return state();
  },
  async buy({ id }) {
    await new Promise((r) => setTimeout(r, 500));
    if (id === 'premium') writeStored(OWNED, '1');
    return { ...state(), status: 'ok' };
  },
  async addListener() {
    return { remove: async () => {} };
  },
};
