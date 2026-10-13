const SPEEDS = [0.75, 1, 1.25, 1.5, 1.75, 2];

// Следующая скорость по кругу.
export const nextSpeed = (current) => SPEEDS[(SPEEDS.indexOf(current) + 1) % SPEEDS.length] ?? 1;

export const fmtSpeed = (speed) => `${speed}×`;
