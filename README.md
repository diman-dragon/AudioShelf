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
Что делает: `npm ci` → `npm run sync` → `gradlew assembleDebug` →
выкладывает `audioshelf-apk` (debug APK) в Artifacts.

В репозиторий обязательно коммитить `package-lock.json` и папку `android/`.

### Подпись (чтобы обновлять приложение, а не ставить заново)

APK подписывается постоянным ключом из GitHub Secrets. Без этого на каждой сборке ключ
был бы новый, и Android не ставил бы APK поверх старого.

Settings → Secrets and variables → Actions → New repository secret:

| Секрет | Значение |
|---|---|
| `KEYSTORE_BASE64` | файл ключа в base64 |
| `KEYSTORE_PASSWORD` | пароль ключа |
| `KEY_ALIAS` | `audioshelf` |

Base64 в PowerShell (результат попадает в буфер обмена):
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes(".\audioshelf-release.keystore")) | Set-Clipboard
```
Файл ключа в репозиторий не коммитить (он в `.gitignore`) и сохранить в надёжном месте:
потеряете ключ — обновления поверх установленной версии станут невозможны.

### Версия

Версия задаётся в одном месте — `package.json` → `"version"`. Из неё берутся и текст
в настройках, и `versionName`, и `versionCode` (`0.1.1` → `101`, `0.2.0` → `200`).
Чтобы выпустить обновление, поднимите версию и запушьте.

## Принципы кода

- Один файл — одна ответственность.
- Файл дорос до 50–60 строк — выносим кусок в отдельную функцию/файл (без фанатизма).
- Экраны регистрируются в `src/screens/index.js`: вкладки и заголовок строятся из него.

## Структура

```
src/
  main.js            точка входа
  app/               оболочка, роутер, системная «Назад»
  screens/           экраны: library, player, settings
  ui/                мелкие компоненты
  core/              утилиты
  styles/            стили по одному файлу на область
```

## Чек-лист Google Play (проверено 13.10.2026)

Обязательное:
- [ ] **targetSdk = 36 (Android 16)** — новые приложения и обновления принимаются только с ним (требование действует с 31.08.2026).
  После `cap add android` проверь `android/variables.gradle`: `targetSdkVersion = 36`.
- [ ] Публикация в формате **AAB** (Build → Generate Signed Bundle).
- [ ] **Play App Signing** включён, ключ загрузки хранится отдельно (`*.jks` в .gitignore).
- [ ] **applicationId** (`com.audioshelf.app`) — после публикации не меняется. Поменяй на свой домен до первой загрузки.
- [ ] Политика конфиденциальности (URL) и форма **Data safety**.
- [ ] Анкета возрастного рейтинга и целевой аудитории.
- [ ] Иконка 512×512, feature graphic 1024×500, минимум 2 скриншота.

Что понадобится позже, когда появится воспроизведение:
- Фоновое воспроизведение — foreground service типа `mediaPlayback`
  (`FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) и уведомление.
- Разрешение на уведомления `POST_NOTIFICATIONS` (Android 13+).
- Доступ к файлам — через системный выбор (SAF) или `READ_MEDIA_AUDIO`.
  Не использовать `MANAGE_EXTERNAL_STORAGE`: Play его почти всегда отклоняет.

Особенности Android 16:
- Edge-to-edge включён принудительно — отступы сделаны через `env(safe-area-inset-*)`.
- Predictive back: кнопка «Назад» обрабатывается через `@capacitor/app`.
  Проверь на устройстве или эмуляторе с Android 16.

Для новых личных аккаунтов разработчика у Google могут быть доп. условия
(закрытое тестирование перед выходом в продакшен) — смотри в Play Console.
