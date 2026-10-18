import './styles/index.css';
import { initStorage } from './core/storage.js';

initStorage().finally(() => import('./start.js'));
