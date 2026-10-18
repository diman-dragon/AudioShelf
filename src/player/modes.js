export const MODES = {
  book: ['continue', 'chapter'],
  album: ['off', 'all', 'one', 'shuffle'],
};

export const EDGES = {
  book: { fadeIn: 0, fadeOut: 0, speech: true },
  album: { fadeIn: 1500, fadeOut: 2500, speech: false },
};

export const nativeMode = (mode) => ({
  repeat: mode === 'all' || mode === 'shuffle' ? 'all' : mode === 'one' ? 'one' : 'off',
  shuffle: mode === 'shuffle',
  pauseAfter: mode === 'chapter',
});
