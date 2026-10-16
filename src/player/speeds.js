export const SPEEDS = [0.75, 1, 1.25, 1.5, 1.75, 2];

export const fmtSpeed = (speed) => `${speed}×`;

// Следующая скорость по кругу.
export const nextSpeed = (speed) => SPEEDS[(SPEEDS.indexOf(speed) + 1) % SPEEDS.length] ?? 1;
