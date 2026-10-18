export function countUnder(files) {
  const under = new Map();
  files.forEach((file) => {
    const parts = file.dir ? file.dir.split('/') : [];
    for (let i = 1; i <= parts.length; i++) {
      const key = parts.slice(0, i).join('/');
      under.set(key, (under.get(key) ?? 0) + 1);
    }
  });
  return under;
}
