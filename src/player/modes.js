// Режимы воспроизведения: у книги и у альбома свои. Первый в списке — по умолчанию.
export const MODES = {
  book: ['continue', 'chapter'], // подряд / пауза после каждой главы
  album: ['off', 'all', 'one', 'shuffle'], // по порядку / повтор всех / повтор трека / вперемешку
};

// Края файла: плавное появление и затухание, мс. У книги — короткие (только без щелчков), у альбома — длинные.
export const EDGES = {
  book: { fadeIn: 300, fadeOut: 700 },
  album: { fadeIn: 1500, fadeOut: 2500 },
};

// Что ждёт нативный плеер.
export const nativeMode = (mode) => ({
  repeat: mode === 'all' || mode === 'shuffle' ? 'all' : mode === 'one' ? 'one' : 'off',
  shuffle: mode === 'shuffle',
  pauseAfter: mode === 'chapter',
});
