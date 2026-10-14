// Какая страница настроек открыта (null — список разделов).
let page = null;

export const getPage = () => page;

function change(next) {
  page = next;
  document.dispatchEvent(new Event('settingspage'));
}

export const openPage = (id) => change(id);
export const closePage = () => change(null);
