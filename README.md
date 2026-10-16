# AudioShelf

Проигрыватель аудиокниг и музыки. Capacitor (Android) + Vite, чистый JS.

## Запуск

```bash
npm install
npx cap add android      # один раз
npm run sync             # сборка веб-части + cap sync
npm run android          # открыть в Android Studio
```

Для разработки в браузере: `npm run dev`.

## Сборка в GitHub Actions

Workflow: `.github/workflows/android.yml` (Node 22, Java 21 — этого требует Capacitor 8).
Что делает: `npm ci` → `npm run sync` → `gradlew assembleDebug` → кладёт APK
`AudioShelf-<версия>.apk` в Artifacts и публикует релиз **«AudioShelf <версия>»**
(тег `v<версия>`) с этим APK. Релиз не создаётся для pull request.

Настраивать ничего не нужно: секреты не требуются.
В репозиторий обязательно коммитить `package-lock.json`, папку `android/`
и ключ `android/app/debug.keystore`.

### Обновление поверх установленного приложения

APK подписывается постоянным ключом `android/app/debug.keystore` (стандартные
debug-данные). Поэтому новая версия ставится поверх старой, без удаления.
Ключ только для личной установки. Для Google Play используется отдельный
ключ загрузки (`store/PUBLISHING.md`).

### Версия и релиз

Версия задаётся в одном месте — `package.json` → `"version"`. Из неё берутся текст
в настройках, `versionName`, `versionCode` (`0.9.4` → `904`, `0.2.0` → `200`), имя APK
и название релиза. Чтобы выпустить обновление, поднимите версию и запушьте.
Если версию не менять, релиз с этим номером обновится новым APK.

## Принципы кода

- Один файл — одна ответственность.
- Файл дорос до 50–60 строк — выносим кусок в отдельную функцию/файл (без фанатизма).
- Экраны регистрируются в `src/screens/index.js`: вкладки и заголовок строятся из него.

## Структура

```
src/
  main.js            точка входа
  app/               оболочка, роутер, системная «Назад», свайпы
  screens/           экраны: library, player, sound, settings
  library/           библиотека: разбор папок на книги и альбомы, фильтр и поиск, обложки, теги, обход папки
  player/            плеер: состояние, прогресс, режимы, закладки, список глав
    chapter-store.js, expand.js  главы внутри файла (MKA, M4B, MP3): кэш оглавления и раскладка книги на главы (читает нативный `ChaptersPlugin`)
  sound/             эквалайзер, пресеты, нормализация громкости (Премиум)
  billing/           Премиум и «чаевые»: покупки, замок на «Звуке», карточки в настройках
  billing/dev-card.js  Ключ разработчика: отпечаток телефона и ввод ключа (проверка в `DevLicense.java`; выдача — отдельный инструмент `AudioShelfLicense`, в проект не входит)
  settings/          страницы настроек (листаются свайпом; «О нас» — ui/about-card.js)
  folder/            JS-обёртки нативных плагинов + заглушки для браузера
  i18n/ theme/       языки (ru/en) и темы (светлая/тёмная)
  ui/                мелкие компоненты (окна, переключатели)
  core/              утилиты
  styles/            стили по одному файлу на область
android/app/src/main/java/com/audioshelf/app/
  Player*, Playback*  плеер на Media3 ExoPlayer, шторка уведомления
  Sound*, Biquad      10-полосный эквалайзер как аудиопроцессор
  Loudness*           фоновое измерение громкости файлов (пауза при слабой батарее)
  Resume*, Position*  память места на диске, откат на 5 с, возобновление после убийства процесса
  PlayerOffload       аппаратное воспроизведение (настройка «Энергосбережение»)
  Billing*, Purchase* Google Play Billing: Премиум и «чаевые» (ID товаров: premium, tip_small, tip_medium, tip_large); соединение, каталог цен, обработка покупок и плагин — отдельные классы
  Folder*, FileOps*   доступ к папкам и файлам (SAF)
```

## Публикация в Google Play

Всё подготовлено, пошаговая инструкция — `store/PUBLISHING.md`.

- `store/listing/` — тексты карточки (RU, EN), `store/graphics/` — иконка и feature graphic, `store/screenshots/` — скриншоты.
- `store/data-safety.md` — ответы для Data safety и деклараций.
- `docs/index.html` — политика конфиденциальности (включить GitHub Pages из `/docs`, вписать e-mail).
- `.github/workflows/release-aab.yml` — ручная сборка подписанного AAB (нужны 4 секрета, см. инструкцию).
