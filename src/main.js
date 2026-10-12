import './styles/index.css';
import { mountApp } from './app/app.js';
import { initBack } from './app/back.js';

mountApp(document.getElementById('app'));
initBack();
