(() => {
  const NAV = [
    ['overview','🏠','Overview','index.html'],
    ['vehicles','🚚','Vehicles','vehicles.html'],
    ['trips','🛣️','Trips','trips.html'],
    ['drivers','🧑‍✈️','Drivers','drivers.html'],
    ['maintenance','🔧','Maintenance','maintenance.html'],
    ['fuel','⛽','Fuel','fuel.html'],
    ['alerts','🔔','Alerts','alerts.html'],
    ['reports','📄','Reports','reports.html'],
    ['geofences','🎯','Geofences','geofences.html'],
    ['settings','⚙️','Settings','settings.html'],
  ];
  function sidebar(active){
    return `<aside><div class="logo"><span class="sq">🛰️</span> FleetPulse</div>
      <nav>${NAV.map(([id,ic,l,h]) =>
        `<a href="${h}" class="${id===active?'active':''}"><span class="ic">${ic}</span> ${l}</a>`).join('')}</nav>
      <div class="health"><h4>Fleet Health</h4>
        <div class="ring" id="hRing"><div id="hScore">–</div></div>
        <div class="st" id="hLabel">–</div>
        <div class="st" style="font-weight:400;font-size:10.5px;color:#9fb3d1">live from pipeline</div>
      </div>
      </aside>`;
  }
  document.addEventListener('DOMContentLoaded', () => {
    const host = document.getElementById('sidebar');
    if(host){
      host.outerHTML = sidebar(document.body.dataset.page || '');
      health(); setInterval(health, 10000);
    }
  });
  async function health(){
    try{
      const [sum, alerts] = await Promise.all([
        fetch('/api/v1/vehicles/summary').then(r=>r.ok?r.json():Promise.reject()),
        fetch('/api/v1/alerts/recent?limit=100').then(r=>r.ok?r.json():Promise.reject())
      ]);
      const recent = alerts.filter(a=>new Date(a.ts).getTime()>Date.now()-36e5).length;
      const score = Math.max(40, 100 - Math.min(40, recent*3) - (sum.total-sum.moving)*0.15);
      const el = document.getElementById('hScore'); if(!el) return;
      el.textContent = score.toFixed(0);
      const ring = document.getElementById('hRing');
      ring.style.setProperty('--p', score);
      ring.style.setProperty('--c', score>=80?'#10b981':score>=60?'#f59e0b':'#ef4444');
      document.getElementById('hLabel').textContent = score>=80?'Good':score>=60?'Watch':'Critical';
    }catch(e){}
  }

  /* ---------- vehicle photo chain (used by vehicles.html) ---------- */
  const TRUCK_SVG = '<svg viewBox="0 0 24 24" width="22" height="22">'
    + '<rect x="2" y="7" width="11" height="8" rx="1" fill="#3b82f6"/>'
    + '<path d="M13 9h4.2c.5 0 .96.25 1.24.66L20.5 12.5V15H13V9Z" fill="#60a5fa"/>'
    + '<circle cx="6" cy="16.8" r="1.8" fill="#1e293b"/>'
    + '<circle cx="16.5" cy="16.8" r="1.8" fill="#1e293b"/></svg>';
  function vClass(make, model){
    const m = `${make ?? ''} ${model ?? ''}`;
    if(/Hiace|Coaster|Rosa|Matatu|Bus/i.test(m))            return 'bus';
    if(/Canter|Dyna|ELF|Hino 300|NPR|NQR|Kuzer/i.test(m))   return 'light';
    if(/FRR|FVR|Fighter|Hino 500|Croner|P280|FTS/i.test(m)) return 'medium';
    return 'heavy';
  }
  function thumbHTML(v){
    const cls = vClass(v.make, v.model);
    return '<img src="img/' + encodeURIComponent(v.vehicleId) + '.jpg"'
         + ' data-cls="' + cls + '" alt="' + (v.vehicleId || '') + '"'
         + ' onerror="window.__thumbErr(this)">';
  }
  window.__thumbErr = function(img){
    if(!img.dataset.step){
      img.dataset.step = '2';
      img.src = 'img/' + img.dataset.cls + '.jpg';
    } else {
      const s = document.createElement('span');
      s.style.display = 'grid'; s.style.placeItems = 'center';
      s.innerHTML = TRUCK_SVG;
      img.replaceWith(s);
    }
  };

  window.FleetUI = {
    esc: s => (s ?? '').replace(/[<>&"]/g, c => ({'<':'&lt;','>':'&gt;','&':'&amp;','"':'&quot;'}[c])),
    fmt: n => n == null ? '–' : Number(n).toFixed(0),
    fmtPlate: id => { const m = /^(K[A-Z]{2})(\d{3}[A-Z])$/.exec(id||''); return m ? m[1]+' '+m[2] : id||''; },
    api: async p => { const r = await fetch(p); if(!r.ok) throw new Error(r.status); return r.json(); },
    vClass, thumbHTML
  };
})();
