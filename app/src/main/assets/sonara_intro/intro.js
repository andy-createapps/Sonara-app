// Sonara welcome intro: the source's welcome.js, run inside the app's own WebView.
(function () {
'use strict';
const P = new URLSearchParams(location.search);
const EDITION = P.get('edition') === 'camellia' ? 'camellia' : 'andy';
const MODE = P.get('mode') || 'run';
const EGG = MODE === 'egg';
const STEPS = EGG ? window.SONARA_EGG : window.SONARA_STEPS[EDITION];
const ASK = window.SONARA_ASK[EDITION];
const $ = (s, r = document) => r.querySelector(s);
const sleep = ms => new Promise(r => setTimeout(r, ms));
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const LOGO = '<svg width="26" height="26" viewBox="0 0 24 24" aria-hidden="true"><g fill="currentColor"><rect x="3" y="9" width="3" height="6" rx="1.5"/><rect x="7.5" y="5" width="3" height="14" rx="1.5"/><rect x="12" y="2.5" width="3" height="19" rx="1.5"/><rect x="16.5" y="6.5" width="3" height="11" rx="1.5"/></g></svg>';
function closeNative() { try { SonaraIntro.close(); } catch (_) {} }

// beat grid for the bundled song (from the source's beat.js)
let bass = null, ons = null;
(function () { const m = window.BEATMAP; if (!m) return; const mx = a => a.reduce((x, y) => Math.max(x, y), 1e-9); const mb = mx(m.bass), mo = mx(m.onset); bass = m.bass.map(v => v / mb); ons = m.onset.map(v => v / mo); })();
function pulseAt(t) {
  const map = window.BEATMAP; if (!map || !bass) return 0;
  const bt = map.beats; let lo = 0, hi = bt.length - 1;
  while (lo < hi) { const m = (lo + hi + 1) >> 1; if (bt[m] <= t) lo = m; else hi = m - 1; }
  const d = t - bt[lo]; const kick = d >= 0 ? Math.exp(-d * 9) : 0;
  const i = Math.max(0, Math.min(bass.length - 1, Math.floor(t * map.envFps)));
  return Math.min(1, kick * 0.75 + bass[i] * 0.35 + ons[i] * 0.2);
}

// ---- background song (bundled with the app, loops from the first drop) ----
let song = null, fadeT = 0, cancelled = false, beatRaf = 0;
async function startSong(set) {
  try {
    const r = { url: 'dracula.m4a', start: 10, loopEnd: 108 }; if (cancelled) return false;
    const a = new Audio(r.url); a.volume = 0; a.preload = 'auto'; a.loop = false;
    await new Promise((ok, no) => { a.addEventListener('canplay', ok, { once: true }); a.addEventListener('error', () => no(new Error('audio error')), { once: true }); setTimeout(() => no(new Error('timeout')), 15000); });
    const start = r.start || 0, end = r.loopEnd || 0;
    try { a.currentTime = start; } catch (_) {}
    a.addEventListener('timeupdate', () => { if (end && a.currentTime >= end) a.currentTime = start; });
    a.addEventListener('ended', () => { a.currentTime = start; a.play().catch(() => {}); });
    if (cancelled) return false;
    await a.play(); song = a; rampTo(0.7, 9000);
    return true;
  } catch (e) { console.warn('welcome song:', e && e.message); set && set(); return false; }
}
function rampTo(v, ms) {
  if (!song) return; clearInterval(fadeT); const a = song, from = a.volume, t0 = Date.now();
  fadeT = setInterval(() => { const k = Math.min(1, (Date.now() - t0) / ms); a.volume = Math.max(0, Math.min(1, from + (v - from) * k)); if (k >= 1) { clearInterval(fadeT); if (v === 0) a.pause(); } }, 50);
}
const stopSong = async () => { rampTo(0, 1800); await sleep(1900); if (song) { try { song.pause(); } catch (_) {} song = null; } };

// the cover, the glow and the card all move with the beat
function beatLoop(root) {
  const cover = $('.wl-cover', root), glow = $('.wl-glow', root), card = $('#wl-card', root), eq = $('.wl-eq', root);
  let sm = 0, flip = 1, lastBig = 0;
  const tick = () => {
    beatRaf = requestAnimationFrame(tick);
    const t = song && !song.paused ? song.currentTime : -1; const target = t >= 0 ? pulseAt(t + 0.04) : 0;
    sm += (target - sm) * (target > sm ? 0.6 : 0.16);
    if (sm > 0.72 && performance.now() - lastBig > 200) { lastBig = performance.now(); flip = -flip; }
    if (cover) cover.style.transform = `scale(${(1 + sm * 0.12).toFixed(3)}) rotate(${(flip * sm * 1.1).toFixed(2)}deg)`;
    if (glow) { glow.style.opacity = (0.25 + sm * 0.75).toFixed(2); glow.style.transform = `scale(${(1 + sm * 0.25).toFixed(3)})`; }
    if (card) card.style.setProperty('--shake', (flip * sm * 1.6).toFixed(2) + 'px');
    if (eq) [...eq.children].forEach((b, i) => { b.style.transform = `scaleY(${(0.25 + Math.min(1, sm * (0.7 + 0.3 * Math.sin(i * 1.7 + performance.now() / 160)) + 0.12)).toFixed(2)})`; });
  };
  tick();
}

// ---- UI (everything fades; nothing is typed out) ----
const fadeIn = (el, html, ms = 800) => {
  if (!el) return Promise.resolve();
  if (html != null) el.innerHTML = html;
  el.classList.add('wl-fade'); el.style.transitionDuration = ms + 'ms'; void el.offsetWidth; el.classList.add('in');
  return sleep(ms * 0.8);
};
const fadeOut = (el, ms = 450) => { if (!el) return Promise.resolve(); el.style.transitionDuration = ms + 'ms'; el.classList.remove('in'); return sleep(ms); };

async function runWelcome() {
  if ($('#welcome')) return; cancelled = false;
    const root = document.createElement('div'); root.id = 'welcome'; root.className = 'wl-intro' + (EGG ? ' egg' : '');
  root.innerHTML = `<div class="wl-bg"><i></i><i></i><i></i></div><div class="wl-logo wl-fade" id="wl-logo">${LOGO}<span>Sonara</span></div>
    <div class="wl-stage"><div class="wl-art wl-fade" id="wl-art"><div class="wl-glow"></div><div class="wl-cover" style="background-image:url('cover.jpg')"></div></div>
    <div class="wl-np wl-fade" id="wl-np"><span class="wl-eq"><i></i><i></i><i></i><i></i></span><span>Dracula (JENNIE Remix) &bull; Tame Impala &times; JENNIE</span></div>
    <div class="wl-card" id="wl-card"></div></div>
    <button class="wl-skip wl-fade" id="wl-skip">Skip intro</button><div class="wl-dots wl-fade" id="wl-dots"></div>`;
  root.style.opacity = '0'; document.body.appendChild(root); void root.offsetWidth; root.style.transition = 'opacity 1.2s ease'; root.style.opacity = '1';
  let skipped = false; const card = $('#wl-card', root), art = $('#wl-art', root);
  const finish = async () => { cancelled = true; document.removeEventListener('keydown', esc_); root.classList.add('out'); const sp = stopSong(); await sleep(900); root.remove(); cancelAnimationFrame(beatRaf); await sp; closeNative(); };
  const esc_ = e => { if (e.key === 'Escape') { skipped = true; finish(); } };
  document.addEventListener('keydown', esc_);
  const sk = $('#wl-skip', root); sk.onclick = () => { skipped = true; finish(); };
  window.__skipIntro = () => { skipped = true; finish(); };
  const dots = $('#wl-dots', root); dots.innerHTML = STEPS.map(() => '<i></i>').join('');
  const dotsSet = i => [...dots.children].forEach((d, k) => d.className = k < i ? 'done' : k === i ? 'on' : '');

  // 1) the song fades in slowly (about nine seconds up to full volume)
  await sleep(900); if (skipped) return;
  const musicOk = EGG ? false : await startSong(() => {}); if (skipped) return;
  if (!EGG) beatLoop(root);
  await sleep(EGG ? 300 : musicOk ? 2600 : 800); if (skipped) return;

  // 2) the cover art fades in, big, in the middle of the screen
  const renderStep = (i) => {
    const s = STEPS[i]; dotsSet(i);
    if (s.q) {
      card.innerHTML = `<div class="wl-kick wl-fade">Question ${STEPS.slice(0, i + 1).filter(x => x.q).length} of ${STEPS.filter(x => x.q).length}</div><h2 class="wl-h wl-fade" id="wl-h">${esc(s.q)}</h2><div class="wl-opts" id="wl-o">${s.opts.map((x, k) => `<button class="wl-opt wl-fade" data-k="${k}">${esc(x[0])}</button>`).join('')}</div><p class="wl-reply wl-fade" id="wl-r"></p><button class="wl-btn wl-fade" id="wl-n">Next</button>`;
    } else {
      card.innerHTML = `<h1 class="wl-big wl-fade" id="wl-h">${s.big}</h1><p class="wl-small wl-fade" id="wl-s">${esc(s.small)}</p><button class="wl-btn wl-fade ${s.last ? 'go' : ''}" id="wl-n">${esc(s.btn)}</button>`;
    }
  };
  renderStep(0); card.classList.add('wl-fade', 'in'); card.style.transitionDuration = '450ms';
  const showStep = async i => {
    const s = STEPS[i];
    await new Promise(async next => {
      if (s.q) {
        await fadeIn($('.wl-kick', card), null, 600); await fadeIn($('#wl-h', card), null, 900); if (skipped) return;
        const o = $('#wl-o', card);
        for (const b of o.children) { fadeIn(b, null, 700); await sleep(220); }
        o.onclick = async e => {
          const b = e.target.closest('.wl-opt'); if (!b || o.dataset.done) return; o.dataset.done = 1; const k = +b.dataset.k;
          [...o.children].forEach(x => { x.disabled = true; if (x !== b) x.style.opacity = '.35'; }); b.classList.add('pick');
          await fadeIn($('#wl-r', card), esc(s.opts[k][1]), 900); const n = $('#wl-n', card); await fadeIn(n, null, 600); n.onclick = () => next();
        };
      } else {
        await fadeIn($('#wl-h', card), null, 1000); await sleep(300); if (skipped) return;
        await fadeIn($('#wl-s', card), null, 1000); await sleep(250);
        const n = $('#wl-n', card); await fadeIn(n, null, 700); n.onclick = () => next();
      }
    });
  };
  if (EGG) {
    fadeIn($('#wl-dots', root), null, 900); fadeIn($('#wl-logo', root), null, 900); fadeIn(sk, null, 900);
    await sleep(400);
  } else {
  // FLIP: measure where the cover will finally sit, then start it big in the centre of the screen
  const r = art.getBoundingClientRect(); const k = Math.min(innerWidth * 0.72, 300) / r.width;
  const dx = innerWidth / 2 - (r.left + r.width / 2), dy = innerHeight * 0.46 - (r.top + r.height / 2);
  art.style.transition = 'none'; art.style.transform = `translate(${dx}px,${dy}px) scale(${k})`;
  await fadeIn(art, null, 2200); if (skipped) return;
  await sleep(1400); if (skipped) return;
  // 3) the cover moves up to its place, 4) the text fades in underneath
  art.style.transition = 'transform 1.6s cubic-bezier(.65,0,.25,1), opacity 2.2s ease'; art.style.transform = 'none';
  await sleep(1500); if (skipped) return;
  fadeIn($('#wl-np', root), null, 900); fadeIn($('#wl-dots', root), null, 900); fadeIn($('#wl-logo', root), null, 900); fadeIn(sk, null, 900);
  await sleep(500);
  }
  for (let i = 0; i < STEPS.length && !skipped; i++) {
    if (i > 0) { await fadeOut(card, 450); renderStep(i); void card.offsetWidth; card.classList.add('in'); }
    await showStep(i);
  }
  if (!skipped) finish();
}


// ---- launch rules (the app decides which mode this launch gets) ----
function ask(title, text, yes, no) {
  return new Promise(res => {
    const el = document.createElement('div'); el.className = 'wl-ask';
    el.innerHTML = `<div class="wl-ask-card"><h3>${esc(title)}</h3><p>${esc(text)}</p><div class="wl-ask-btns"><button class="wl-btn go" data-v="1">${esc(yes)}</button><button class="wl-btn ghost" data-v="0">${esc(no)}</button></div></div>`;
    document.body.appendChild(el);
    el.onclick = e => { const b = e.target.closest('[data-v]'); if (!b) return; el.remove(); res(b.dataset.v === '1'); };
  });
}
window.__back = () => { if (window.__skipIntro) { window.__skipIntro(); } else { closeNative(); } return true; };
(async function boot() {
  const bg = document.createElement('div'); bg.className = 'wl-bg'; bg.style.position = 'fixed'; bg.innerHTML = '<i></i><i></i><i></i>'; document.body.appendChild(bg);
  if (MODE === 'ask2' || MODE === 'ask3') {
    const a = ASK[MODE === 'ask2' ? 'n2' : 'n3'];
    if (await ask(a[0], a[1], a[2], a[3])) { bg.remove(); runWelcome(); } else closeNative();
  } else { bg.remove(); runWelcome(); }
})();
})();
