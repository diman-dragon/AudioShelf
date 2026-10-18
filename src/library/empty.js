import { el, button, image } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { navigate } from '../app/router.js';
import libraryEmoji from '../assets/emoji/1f4da.svg';

const TEXT = {
  nofolder: ['lib.noFolder', 'lib.noFolderHint'],
  error: ['lib.error', 'lib.errorHint'],
  unscanned: ['lib.unscanned', 'lib.unscannedHint'],
  ok: ['lib.empty', 'lib.emptyHint'],
};

export function createEmpty() {
  const node = el('div', 'empty');
  const title = el('div', 'empty-title');
  const hint = el('div', 'muted');
  const go = bindText(button('btn', () => navigate('settings')), 'lib.choose');
  node.append(image('em', libraryEmoji), title, hint, go);
  return {
    node,
    show(status) {
      const [a, b] = TEXT[status];
      bindText(title, a);
      bindText(hint, b);
      go.hidden = status !== 'nofolder' && status !== 'error';
    },
  };
}
