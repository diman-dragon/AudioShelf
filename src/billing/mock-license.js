// Заглушка для браузера: отпечаток фиксированный, подходит код TEST.
export const mockLicense = {
  async check({ code }) {
    return { fingerprint: 'WEBM-OCKD-EVAA-AAAA', valid: String(code || '').trim() === 'TEST' };
  },
};
