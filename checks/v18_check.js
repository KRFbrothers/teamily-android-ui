const { chromium } = require('/app/node_modules/playwright');
const path = require('path');

const TARGET = process.env.TARGET || 'file://' + path.resolve('webpages/m3-prototype_v18/index.html');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1200 } });
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  page.on('console', m => { if (m.type() === 'error') errors.push('console: ' + m.text()); });

  await page.goto(TARGET, { waitUntil: 'load' });
  await page.waitForTimeout(1200);

  const out = [];
  const ck = (n, ok, extra) => out.push([n, !!ok, extra === undefined ? '' : String(extra)]);
  const ev = (fn, arg) => page.evaluate(fn, arg);

  const openSettings = async () => {
    await ev(() => {
      const p = document.querySelector('.phone[data-mode="agent"]');
      const f = p.querySelector('[data-filter]'); if (f) f.click();
    });
    await page.waitForTimeout(250);
    await ev(() => {
      const p = document.querySelector('.phone[data-mode="agent"]');
      const m = p.querySelector('[data-pub-menu]'); if (m) m.click();
    });
    await page.waitForTimeout(400);
  };
  const openChannels = async () => {
    await ev(() => {
      const rows = [...document.querySelectorAll('.phone[data-mode="agent"] .set-row')];
      const r = rows.find(x => /Channels for Dua Agent/.test(x.textContent));
      if (r) r.click();
    });
    await page.waitForTimeout(400);
  };

  await openSettings();
  await openChannels();

  // ---------- 1. channels screen + timestamps ----------
  const ch = await ev(() => {
    const pv = document.querySelector('.phone[data-mode="agent"] [data-pubview]');
    const rows = [...pv.querySelectorAll('[data-chan-drag]')];
    return {
      open: pv.classList.contains('open'),
      title: (pv.querySelector('.pub-bar-t') || {}).textContent.trim(),
      n: rows.length,
      rows: rows.map(r => ({
        key: r.getAttribute('data-chan-drag'),
        label: (r.querySelector('.set-title') || {}).textContent,
        when: (r.querySelector('.set-when') || {}).textContent || null,
        hasGrip: !!r.querySelector('[data-chan-grip]'),
        hasSwitch: !!r.querySelector('.set-sw'),
        swOn: (r.querySelector('.set-sw') || {}).className || ''
      }))
    };
  });
  ck('channels screen open', ch.open);
  ck('channels title', ch.title === 'Channels for Dua Agent', ch.title);
  ck('6 channel rows', ch.n === 6, 'n=' + ch.n);
  ck('every channel has a last-message stamp', ch.rows.every(r => r.when && r.when.trim()), JSON.stringify(ch.rows.map(r => r.when)));
  ck('every channel has a drag grip', ch.rows.every(r => r.hasGrip));
  ck('every channel still has its switch', ch.rows.every(r => r.hasSwitch));
  ck('stamps are distinct', new Set(ch.rows.map(r => r.when)).size === 6, ch.rows.map(r => r.when).join('|'));

  const stampStyle = await ev(() => {
    const pv = document.querySelector('.phone[data-mode="agent"] [data-pubview]');
    const w = pv.querySelector('.set-when');
    const cs = getComputedStyle(w);
    return { fs: cs.fontSize, fw: cs.fontWeight, color: cs.color, tab: cs.fontVariantNumeric };
  });
  ck('stamp is small + tabular', stampStyle.fs === '12px' && /tabular/.test(stampStyle.tab), JSON.stringify(stampStyle));

  const orderBefore = ch.rows.map(r => r.key);

  // ---------- 2. drag a channel row ----------
  const dragStart = await ev(() => {
    const p = document.querySelector('.phone[data-mode="agent"]');
    const rows = [...p.querySelectorAll('[data-chan-drag]')];
    const a = rows[0], b = rows[1];
    const grip = a.querySelector('[data-chan-grip]');
    const ra = a.getBoundingClientRect(), rb = b.getBoundingClientRect();
    const fire = (el, type, x, y) => el.dispatchEvent(new PointerEvent(type, { bubbles: true, cancelable: true, clientX: x, clientY: y, pointerId: 1, isPrimary: true, button: 0, buttons: 1 }));
    fire(grip, 'pointerdown', ra.left + 20, ra.top + ra.height / 2);
    fire(grip, 'pointermove', ra.left + 20, ra.top + ra.height / 2 + 12);
    window.__a = a; window.__grip = grip; window.__rb = { x: rb.left + 20, y: rb.top + rb.height / 2 + 4 };
    return { cls: a.classList.contains('dragging') };
  });
  await page.waitForTimeout(400); // let the 180ms lift transition settle
  const dragStart2 = await ev(() => {
    const a = window.__a;
    const cs = getComputedStyle(a);
    return { cls: a.classList.contains('dragging'), tf: cs.transform, sh: cs.boxShadow, z: cs.zIndex, cur: cs.cursor };
  });
  ck('channel drag lift class', dragStart2.cls);
  ck('channel lift scale', /matrix\(1\.0[0-9]/.test(dragStart2.tf || ''), dragStart2.tf);
  ck('channel lift shadow', /rgba\(0, 0, 0, 0\.4/.test(dragStart2.sh || ''), (dragStart2.sh || '').slice(0, 40));
  ck('channel lift z-index 2', dragStart2.z === '2', dragStart2.z);
  ck('channel lift cursor grabbing', dragStart2.cur === 'grabbing', dragStart2.cur);

  const midDrag = await ev(() => {
    const grip = window.__grip, rb = window.__rb;
    const fire = (el, type, x, y) => el.dispatchEvent(new PointerEvent(type, { bubbles: true, cancelable: true, clientX: x, clientY: y, pointerId: 1, isPrimary: true, button: 0, buttons: 1 }));
    fire(grip, 'pointermove', rb.x, rb.y);
    const p = document.querySelector('.phone[data-mode="agent"]');
    return { shifted: p.querySelectorAll('.conn-shift').length };
  });
  ck('neighbour rows shift (FLIP)', midDrag.shifted >= 1, 'shifted=' + midDrag.shifted);

  const dropped = await ev(() => {
    const grip = window.__grip, rb = window.__rb;
    const fire = (el, type, x, y) => el.dispatchEvent(new PointerEvent(type, { bubbles: true, cancelable: true, clientX: x, clientY: y, pointerId: 1, isPrimary: true, button: 0, buttons: 1 }));
    fire(grip, 'pointerup', rb.x, rb.y);
    const p = document.querySelector('.phone[data-mode="agent"]');
    const rows = [...p.querySelectorAll('[data-chan-drag]')];
    return {
      order: rows.map(r => r.getAttribute('data-chan-drag')),
      inline: rows.map(r => r.getAttribute('style') || '').join(''),
      dragging: p.querySelectorAll('.dragging').length,
      switches: rows.map(r => (r.querySelector('.set-sw') || {}).className || '')
    };
  });
  ck('channel order changed by drag', dropped.order.join(',') !== orderBefore.join(','), dropped.order.join(','));
  ck('no inline style left behind', dropped.inline === '', dropped.inline.slice(0, 60));
  ck('no row left lifted', dropped.dragging === 0, 'dragging=' + dropped.dragging);
  ck('drag did not flip a switch', dropped.switches.join('|') === ch.rows.map(r => r.swOn).join('|'), dropped.switches.join('|'));

  const saved = await ev(() => {
    const raw = localStorage.getItem('m3-conn-state');
    const b = JSON.parse(raw || '{}');
    return { chanOrder: b.chanOrder || null, chan: b.chan || null };
  });
  ck('chanOrder persisted', Array.isArray(saved.chanOrder) && saved.chanOrder.join(',') === dropped.order.join(','), JSON.stringify(saved.chanOrder));

  // ---------- 3. reload keeps the channel order ----------
  await page.reload({ waitUntil: 'load' });
  await page.waitForTimeout(1000);
  await openSettings();
  await openChannels();
  const afterReload = await ev(() => {
    const pv = document.querySelector('.phone[data-mode="agent"] [data-pubview]');
    return [...pv.querySelectorAll('[data-chan-drag]')].map(r => r.getAttribute('data-chan-drag'));
  });
  ck('channel order survives reload', afterReload.join(',') === dropped.order.join(','), afterReload.join(','));

  // ---------- 4. channel switch still works after reorder ----------
  const sw = await ev(() => {
    const pv = document.querySelector('.phone[data-mode="agent"] [data-pubview]');
    const row = pv.querySelector('[data-chan-drag="slack"]');
    const before = row.querySelector('.set-sw').classList.contains('on');
    row.click();
    const after = row.querySelector('.set-sw').classList.contains('on');
    return { before, after, aria: row.querySelector('.set-sw').getAttribute('aria-checked') };
  });
  ck('channel switch still toggles', sw.before !== sw.after, JSON.stringify(sw));
  ck('switch aria follows', sw.aria === String(sw.after), sw.aria);

  // ---------- 5. connectors drag still works (regression) ----------
  await ev(() => {
    const p = document.querySelector('.phone[data-mode="agent"]');
    const b = p.querySelector('[data-set-sub-back]'); if (b) b.click();
  });
  await page.waitForTimeout(300);
  await ev(() => {
    const rows = [...document.querySelectorAll('.phone[data-mode="agent"] .set-row')];
    const r = rows.find(x => /App Connectors/.test(x.textContent));
    if (r) r.click();
  });
  await page.waitForTimeout(400);

  const connDrag = await ev(() => {
    const p = document.querySelector('.phone[data-mode="agent"]');
    const rows = [...p.querySelectorAll('[data-conn-drag]')];
    if (rows.length < 3) return { ok: false, n: rows.length };
    const a = rows[0], b = rows[1];
    const grip = a.querySelector('[data-conn-grip]');
    const ra = a.getBoundingClientRect(), rb = b.getBoundingClientRect();
    const fire = (el, type, x, y) => el.dispatchEvent(new PointerEvent(type, { bubbles: true, cancelable: true, clientX: x, clientY: y, pointerId: 1, isPrimary: true, button: 0, buttons: 1 }));
    const before = rows.map(r => r.getAttribute('data-conn-drag'));
    fire(grip, 'pointerdown', ra.left + 20, ra.top + ra.height / 2);
    fire(grip, 'pointermove', ra.left + 20, ra.top + ra.height / 2 + 12);
    const cls = a.classList.contains('dragging');
    fire(grip, 'pointermove', rb.left + 20, rb.top + rb.height / 2 + 4);
    fire(grip, 'pointerup', rb.left + 20, rb.top + rb.height / 2 + 4);
    const after = [...p.querySelectorAll('[data-conn-drag]')].map(r => r.getAttribute('data-conn-drag'));
    return { ok: true, n: rows.length, cls, before, after };
  });
  ck('connector rows present', connDrag.ok, 'n=' + connDrag.n);
  ck('connector drag still lifts', connDrag.cls);
  ck('connector drag still reorders', connDrag.before && connDrag.before.join(',') !== connDrag.after.join(','), (connDrag.after || []).join(','));

  // ---------- 6. v17 features intact ----------
  const feat = await ev(() => {
    const p = document.querySelector('.phone[data-mode="agent"]');
    const pv = p.querySelector('[data-pubview]');
    const offAll = pv.querySelector('[data-conn-off-all]');
    const sub = offAll ? (offAll.querySelector('.set-sub') || {}).textContent : null;
    const pills = [...pv.querySelectorAll('[data-conn-pill]')];
    const onN = pills.filter(x => x.getAttribute('aria-pressed') === 'true').length;
    return { sub, onN, pills: pills.length };
  });
  ck('live connected-app count matches pills', feat.sub && feat.sub.indexOf(String(feat.onN)) === 0, feat.sub + ' vs ' + feat.onN);

  const undo = await ev(() => {
    const p = document.querySelector('.phone[data-mode="agent"]');
    const pv = p.querySelector('[data-pubview]');
    pv.querySelector('[data-conn-off-all]').click();
    return { dialog: !!pv.querySelector('[data-conn-confirm]') };
  });
  await page.waitForTimeout(300);
  const undo2 = await ev(() => {
    const p = document.querySelector('.phone[data-mode="agent"]');
    const pv = p.querySelector('[data-pubview]');
    const ok = pv.querySelector('[data-conn-confirm-ok]'); if (ok) ok.click();
    return { snack: !!pv.querySelector('.conn-snack'), undoBtn: !!pv.querySelector('[data-conn-undo]') };
  });
  await page.waitForTimeout(300);
  const undo3 = await ev(() => {
    const p = document.querySelector('.phone[data-mode="agent"]');
    const pv = p.querySelector('[data-pubview]');
    const u = pv.querySelector('[data-conn-undo]'); if (u) u.click();
    const pills = [...pv.querySelectorAll('[data-conn-pill]')];
    return { on: pills.filter(x => x.getAttribute('aria-pressed') === 'true').length };
  });
  ck('disconnect-all confirm dialog', undo.dialog);
  ck('undo snackbar appears', undo2.snack && undo2.undoBtn, JSON.stringify(undo2));
  ck('undo restores connectors', undo3.on === feat.onN, undo3.on + ' vs ' + feat.onN);

  const brand = await ev(() => {
    const p = document.querySelector('.phone[data-mode="agent"]');
    const ph = p.querySelector('.ask-pill, [data-ask-pill]');
    const txt = document.body.textContent;
    return {
      title: document.title,
      dua: /Ask Dua Gen AI/.test(txt),
      teamily: /Teamily/.test(txt),
      tabs: [...new Set([...document.querySelectorAll('button[data-mode]')].map(b => b.textContent.trim()))]
    };
  });
  ck('title is Dua Gen AI', /Dua Gen AI/.test(brand.title), brand.title);
  ck('composer placeholder Ask Dua Gen AI', brand.dua);
  ck('no Teamily branding left', !brand.teamily);
  ck('tabs Dua / Agent', brand.tabs.join(',').indexOf('Dua') !== -1 && brand.tabs.join(',').indexOf('Agent') !== -1, brand.tabs.join(','));

  ck('no page errors', errors.length === 0, errors.slice(0, 3).join(' | '));

  // ---------- report ----------
  let pass = 0;
  for (const [n, ok, extra] of out) {
    if (ok) pass++;
    console.log((ok ? 'PASS' : 'FAIL') + '  ' + n + (extra ? '   [' + extra + ']' : ''));
  }
  console.log('\n' + pass + '/' + out.length + ' passed');
  await browser.close();
  process.exit(pass === out.length ? 0 : 1);
})();
