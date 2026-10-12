import { SCREENS } from '../screens/index.js';
import { createHeader } from '../ui/header.js';
import { createHost } from '../ui/screen-host.js';
import { createTabbar } from '../ui/tabbar.js';
import { setAccent } from '../ui/accent.js';
import { getScreen, navigate, onNavigate } from './router.js';
import { initSwipe } from './swipe.js';

const indexOf = (id) => SCREENS.findIndex((s) => s.id === id);

// Собирает оболочку: шапка + экраны + нижние вкладки + свайпы.
export function mountApp(root) {
  const header = createHeader();
  const host = createHost(SCREENS);
  const tabs = createTabbar(SCREENS, navigate);
  root.append(header.node, host.node, tabs.node);

  let prev = indexOf(getScreen());
  const apply = (id) => {
    const i = indexOf(id);
    setAccent(SCREENS[i].accent);
    header.setTitle(SCREENS[i].title);
    host.show(id, i - prev);
    tabs.setActive(id);
    prev = i;
  };

  onNavigate(apply);
  apply(getScreen());

  initSwipe(host.node, (step) => {
    const next = SCREENS[indexOf(getScreen()) + step];
    if (next) navigate(next.id);
  });
}
