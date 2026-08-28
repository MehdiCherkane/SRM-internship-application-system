/* =========================================================
   SRM STAGE – Khénifra | Home Page Script
   ========================================================= */

'use strict';

/* ── Motion preference ── */
const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/* ── Page Loader ── */
window.addEventListener('load', () => {
  const loader = document.getElementById('page-loader');
  if (!loader) return;
  setTimeout(() => loader.classList.add('hidden'), 600);
});

/* ── Scroll-to-top ── */
const scrollTopBtn = document.getElementById('scroll-top');
window.addEventListener('scroll', () => {
  scrollTopBtn?.classList.toggle('visible', window.scrollY > 300);
}, { passive: true });
scrollTopBtn?.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));

/* ── Mobile Menu ── */
const hamburger = document.getElementById('hamburger-btn');
const mobileMenu = document.getElementById('mobile-menu');
const mobileOverlay = document.getElementById('mobile-overlay');
const mobileClose = document.getElementById('mobile-menu-close');

function openMenu() {
  hamburger?.classList.add('active');
  mobileMenu?.classList.add('open');
  mobileOverlay?.classList.add('open');
  document.body.style.overflow = 'hidden';
  hamburger?.setAttribute('aria-expanded', 'true');
}

function closeMenu() {
  hamburger?.classList.remove('active');
  mobileMenu?.classList.remove('open');
  mobileOverlay?.classList.remove('open');
  document.body.style.overflow = '';
  hamburger?.setAttribute('aria-expanded', 'false');
}

hamburger?.addEventListener('click', () => {
  if (mobileMenu?.classList.contains('open')) {
    closeMenu();
  } else {
    openMenu();
  }
});

mobileClose?.addEventListener('click', closeMenu);
mobileOverlay?.addEventListener('click', closeMenu);

document.querySelectorAll('.mobile-link').forEach(link => {
  link.addEventListener('click', closeMenu);
});

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape' && mobileMenu?.classList.contains('open')) {
    closeMenu();
  }
});

/* ── Hero Slider ── */
(function initSlider() {
  const slides = document.querySelectorAll('.hero-slider .slide');
  if (!slides.length) return;
  const dotsWrap = document.getElementById('slider-dots');
  const prevBtn = document.getElementById('slider-prev');
  const nextBtn = document.getElementById('slider-next');
  const total = slides.length;
  let current = 0;
  let timer = null;

  const AUTOPLAY_MS = 6000;

  slides.forEach((_, i) => {
    const dot = document.createElement('button');
    dot.setAttribute('role', 'tab');
    dot.setAttribute('aria-label', 'Diapositive ' + (i + 1));
    if (i === 0) dot.classList.add('active');
    dot.addEventListener('click', () => goTo(i));
    dotsWrap?.appendChild(dot);
  });

  const dots = dotsWrap ? Array.from(dotsWrap.children) : [];

  function goTo(index) {
    current = (index + total) % total;
    slides.forEach((s, i) => s.classList.toggle('active', i === current));
    dots.forEach((d, i) => d.classList.toggle('active', i === current));
    restart();
  }

  function next() { goTo(current + 1); }
  function prev() { goTo(current - 1); }

  function restart() {
    if (timer) clearInterval(timer);
    if (prefersReducedMotion) return;
    timer = setInterval(next, AUTOPLAY_MS);
  }

  nextBtn?.addEventListener('click', next);
  prevBtn?.addEventListener('click', prev);

  document.querySelector('.hero-slider')?.addEventListener('mouseenter', () => {
    if (timer) clearInterval(timer);
  });
  document.querySelector('.hero-slider')?.addEventListener('mouseleave', restart);

  restart();
})();

/* ── Tabs (Domaines de stage) ── */
(function initTabs() {
  const btns = document.querySelectorAll('.tab-btn');
  const panels = document.querySelectorAll('.tab-panel');
  if (!btns.length) return;

  function activate(index) {
    btns.forEach(b => b.classList.toggle('active', Number(b.dataset.tab) === index));
    panels.forEach(p => p.classList.toggle('active', Number(p.dataset.panel) === index));
  }

  btns.forEach(btn => {
    btn.addEventListener('click', () => activate(Number(btn.dataset.tab)));
  });

  // Submenu shortcuts: data-tab links in dropdowns and mobile menu
  document.querySelectorAll('.submenu a[data-tab], .mobile-menu a[data-tab]').forEach(link => {
    link.addEventListener('click', () => {
      activate(Number(link.dataset.tab));
    });
  });
})();

/* ── Accordion (Espace Candidat) ── */
(function initAccordion() {
  document.querySelectorAll('.accordion-head').forEach(head => {
    head.addEventListener('click', () => {
      const item = head.closest('.accordion-item');
      const body = item.querySelector('.accordion-body');
      const isOpen = item.classList.contains('active');

      // Close all
      document.querySelectorAll('.accordion-item.active').forEach(other => {
        other.classList.remove('active');
        const otherBody = other.querySelector('.accordion-body');
        if (otherBody) otherBody.style.maxHeight = null;
        other.querySelector('.accordion-head')?.setAttribute('aria-expanded', 'false');
      });

      if (!isOpen) {
        item.classList.add('active');
        body.style.maxHeight = body.scrollHeight + 'px';
        head.setAttribute('aria-expanded', 'true');
      }
    });
  });

  // Open first item on load
  const first = document.querySelector('.accordion-item.active');
  const firstBody = first?.querySelector('.accordion-body');
  if (first && firstBody) firstBody.style.maxHeight = firstBody.scrollHeight + 'px';

  let resizeTimer;
  window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
      document.querySelectorAll('.accordion-item.active .accordion-body').forEach(body => {
        body.style.maxHeight = body.scrollHeight + 'px';
      });
    }, 150);
  });
})();

/* ── Smooth anchor scroll ── */
document.querySelectorAll('a[href^="#"]').forEach(link => {
  link.addEventListener('click', e => {
    const target = document.querySelector(link.getAttribute('href'));
    if (!target) return;
    e.preventDefault();
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
});

/* ── Scroll Reveal ── */
const revealObserver = new IntersectionObserver((entries) => {
  entries.forEach(e => {
    if (e.isIntersecting) {
      e.target.classList.add('visible');
      revealObserver.unobserve(e.target);
    }
  });
}, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' });

document.querySelectorAll('.reveal, .reveal-left, .reveal-right')
  .forEach(el => revealObserver.observe(el));

/* ── Animated Counters ── */
function animateCount(el) {
  const target = parseFloat(el.dataset.target);
  const suffix = el.dataset.suffix || '';
  if (prefersReducedMotion) { el.textContent = target + suffix; return; }
  const duration = 1800;
  const start = performance.now();

  const tick = (now) => {
    const elapsed = now - start;
    const progress = Math.min(elapsed / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    const value = Math.round(eased * target);
    el.textContent = value + suffix;
    if (progress < 1) requestAnimationFrame(tick);
  };
  requestAnimationFrame(tick);
}

const counterObserver = new IntersectionObserver((entries) => {
  entries.forEach(e => {
    if (e.isIntersecting) {
      animateCount(e.target);
      counterObserver.unobserve(e.target);
    }
  });
}, { threshold: 0.5 });

document.querySelectorAll('[data-target]').forEach(el => counterObserver.observe(el));