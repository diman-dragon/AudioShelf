const DISC = /^(cd|disc|disk|диск|part|часть|том|vol(?:ume)?\.?)[\s_-]*\d+$/i;
const STANDALONE = /\.m4b$/i;
const BIG = 40 * 1048576;
const NUMBERED = 0.8;

export function groupDir(dir) {
  const parts = dir.split('/');
  return parts.length > 1 && DISC.test(parts[parts.length - 1]) ? parts.slice(0, -1).join('/') : dir;
}

export function splitGroup(files) {
  const whole = files.filter((f) => STANDALONE.test(f.name));
  const rest = files.filter((f) => !STANDALONE.test(f.name));
  const numbered = rest.filter((f) => /\d/.test(f.name.replace(/\.[^.]+$/, ''))).length;
  const avg = rest.reduce((sum, f) => sum + f.size, 0) / (rest.length || 1);
  const separate = rest.length > 1 && avg >= BIG && numbered / rest.length < NUMBERED;
  return [...whole.map((f) => [f]), ...(separate ? rest.map((f) => [f]) : rest.length ? [rest] : [])];
}
