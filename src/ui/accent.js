// Акцентный цвет приложения меняется вместе с экраном.
export function setAccent(color) {
  document.documentElement.style.setProperty('--accent', color);
}
