import { loadCover } from './cover-queue.js';

// Обложку грузим, только когда строка подъехала к экрану.
const observer = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      observer.unobserve(entry.target);
      const img = entry.target.querySelector('.pv');
      img.onload = () => img.classList.add('ready');
      loadCover(entry.target._file, img);
    });
  },
  { rootMargin: '200px' },
);

export const watchCover = (item) => observer.observe(item);
