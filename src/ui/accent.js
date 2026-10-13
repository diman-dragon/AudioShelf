// Какой экран открыт — от этого CSS выбирает акцентный цвет (см. styles/theme*.css).
export function setAccentScreen(id) {
  document.documentElement.dataset.screen = id;
}
