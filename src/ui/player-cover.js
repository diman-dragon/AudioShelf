import { createArt } from './art.js';
import playerArt from '../assets/art/player.webp';

// Заглушка обложки; позже сюда подставим картинку книги/альбома.
export function createCover() {
  return createArt(playerArt, 260, 'cover');
}
