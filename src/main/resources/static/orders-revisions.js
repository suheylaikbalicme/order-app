const $ = (id) => document.getElementById(id);
const h = (window.MrCrm && window.MrCrm.h) ? window.MrCrm.h : null;
if (!h) { throw new Error("MrCrm helpers not loaded"); }

function esc(s) {
  return String(s ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function setMsg(text) {
  const el = $("msg");
  if (!el) return;
  el.textContent = String(text ?? "");
  el.style.display = el.textContent ? "block" : "none";
}

function prettySnapshot(snap) {
  const s = String(snap || "");
  if (!s.trim()) return "";
  try {
    const obj = JSON.parse(s);
    return JSON.stringify(obj, null, 2);
  } catch (e) {
    return s;
  }
}

function openSnapshot(title, snap) {
  const m = document.getElementById('snapModal');
  const t = document.getElementById('snapTitle');
  const p = document.getElementById('snapPre');
  if (t) t.textContent = title || 'Snapshot';
  if (p) p.textContent = prettySnapshot(snap);
  if (m) m.style.display = 'block';
}

function tryParseSnapshot(snap) {
  const s = String(snap || "");
  if (!s.trim()) return null;
  try {
    return JSON.parse(s);
  } catch (e) {
    return null;
  }
}

function n(v) {
  if (v === null || v === undefined || v === "") return null;
  const x = h.num(v);
  return Number.isFinite(x) ? x : null;
}

function money(v) {
  const x = n(v);
  if (x === null) return "-";
  return h.money(x);
}

function statusBadge(status) {
  const s = String(status || "").toUpperCase();
  if (!s) return `<span class="badge">-</span>`;
  let cls = "info";
  if (s === "APPROVED") cls = "approved";
  else if (s === "DRAFT") cls = "draft";
  else if (s === "REVISED" || s === "REVIZED") cls = "warning";
  else if (s === "SUBMITTED" || s === "PENDING") cls = "pending";
  else if (s === "CANCELLED" || s === "CANCELED") cls = "cancelled";
  return `<span class="badge ${cls}">${esc(s)}</span>`;
}

function buildSummary(snapshot) {
  if (!snapshot) {
    return `<div class="rev-summary"><span class="badge danger">SNAPSHOT YOK</span></div>`;
  }
  const itemsCount = Array.isArray(snapshot.items) ? snapshot.items.length : 0;
  const cur = snapshot.currency ? String(snapshot.currency) : "-";
  return `
    <div class="rev-summary">
      ${statusBadge(snapshot.status)}
      <span class="badge">${esc(cur)}</span>
      <span class="rev-meta">Genel: <b>${money(snapshot.grandTotal)}</b></span>
      <span class="rev-meta">KDV: <b>${money(snapshot.vatTotal)}</b></span>
      <span class="rev-meta">Kalem: <b>${itemsCount}</b></span>
    </div>
  `;
}

function buildChanges(curr, prev) {
  if (!curr) return `<span class="muted">-</span>`;
  if (!prev) return `<span class="badge info">İLK KAYIT</span>`;

  const badges = [];
  if (String(curr.status || "") !== String(prev.status || "")) badges.push(`<span class="badge warning">DURUM</span>`);

  const currItems = Array.isArray(curr.items) ? curr.items.length : 0;
  const prevItems = Array.isArray(prev.items) ? prev.items.length : 0;
  if (currItems !== prevItems) badges.push(`<span class="badge info">KALEM</span>`);

  const gt = n(curr.grandTotal);
  const pgt = n(prev.grandTotal);
  if (gt !== null && pgt !== null && Math.abs(gt - pgt) > 0.0001) badges.push(`<span class="badge success">TOPLAM</span>`);

  const vat = n(curr.vatTotal);
  const pvat = n(prev.vatTotal);
  if (vat !== null && pvat !== null && Math.abs(vat - pvat) > 0.0001) badges.push(`<span class="badge success">KDV</span>`);

  if (String(curr.currency || "") !== String(prev.currency || "")) badges.push(`<span class="badge info">PARA</span>`);
  const er = n(curr.exchangeRate);
  const per = n(prev.exchangeRate);
  if (er !== null && per !== null && Math.abs(er - per) > 0.0001) badges.push(`<span class="badge info">KUR</span>`);

  if (!badges.length) return `<span class="badge">DEĞİŞİKLİK YOK</span>`;
  return `<div class="rev-changes">${badges.join(" ")}</div>`;
}

(async function init() {
  try {
    const orderIdEl = $("orderId");
    if (!orderIdEl) throw new Error("orderId bulunamadı");
    const orderId = orderIdEl.value;
    const r = await fetch(`/api/orders/${orderId}/revisions`);
    let rows = [];
    if (!r.ok) {
      let msg = `${r.status} ${r.statusText}`;
      try {
        const err = await r.json();
        msg = err?.message || msg;
      } catch (e) {}
      throw new Error(msg);
    }
    rows = await r.json();
    if (!Array.isArray(rows)) rows = [];

    const tb = document.querySelector("#revTable tbody");
    const revTable = document.getElementById("revTable");

    // Sort toggle: default assumes API returns newest -> oldest
    let sortNewestFirst = true;

    function ensureSortToggle() {
      if (!revTable) return;
      if (document.getElementById("revSortToggle")) return;

      const bar = document.createElement("div");
      bar.className = "rev-toolbar";
      bar.style.display = "flex";
      bar.style.alignItems = "center";
      bar.style.justifyContent = "space-between";
      bar.style.gap = "12px";
      bar.style.margin = "10px 0";

      const left = document.createElement("div");
      left.className = "muted";
      left.textContent = "Sıralama";

      const btn = document.createElement("button");
      btn.id = "revSortToggle";
      btn.type = "button";
      btn.className = "btn ghost";
      btn.textContent = "Yeni → Eski";
      btn.addEventListener("click", () => {
        sortNewestFirst = !sortNewestFirst;
        btn.textContent = sortNewestFirst ? "Yeni → Eski" : "Eski → Yeni";
        render();
      });

      bar.appendChild(left);
      bar.appendChild(btn);
      revTable.parentNode.insertBefore(bar, revTable);
    }

    function render() {
      if (!tb) return;
      tb.innerHTML = "";

      const list = sortNewestFirst ? rows.slice() : rows.slice().reverse();

      list.forEach((x, idx) => {
      const tr = document.createElement("tr");
      const snap = x.snapshot || "";
      const short = snap.length > 120 ? snap.slice(0, 120) + "..." : snap;

      const curr = tryParseSnapshot(snap);
      const prev = idx + 1 < list.length ? tryParseSnapshot(list[idx + 1]?.snapshot) : null;

      const hasSnap = String(snap || "").trim().length > 0;
      tr.innerHTML = `
        <td>${esc(x.revisionNo ?? "")}</td>
        <td>${esc(x.revisedAt ?? "")}</td>
        <td>${esc(x.revisedByUsername ?? "")}</td>
        <td style="min-width: 260px;">${esc(x.reason ?? "")}</td>
        <td style="min-width: 360px;">${buildSummary(curr)}</td>
        <td style="min-width: 180px;">${buildChanges(curr, prev)}</td>
        <td style="min-width: 240px;">
          <button class="btn ghost" type="button" data-snap="1" ${hasSnap ? "" : "disabled"}>${hasSnap ? "Görüntüle" : "Yok"}</button>
          <div class="muted" style="max-width:520px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${esc(short)}</div>
        </td>
      `;
      tb.appendChild(tr);

      const btn = tr.querySelector('button[data-snap]');
      if (btn && hasSnap) btn.addEventListener('click', () => openSnapshot(`Sipariş #${orderId} - Rev.${x.revisionNo ?? ""}`, snap));
      });
    }

    ensureSortToggle();
    render();

    if (!rows.length) setMsg("Revize geçmişi bulunamadı.");
  } catch (e) {
    setMsg(e.message || String(e));
  }
})();