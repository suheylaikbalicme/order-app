(function () {
  const $ = (id) => document.getElementById(id);
  const H = (window.MrCrm && window.MrCrm.h) ? window.MrCrm.h : null;

  const qEl = $("q");
  const limitEl = $("limit");
  const btnLoad = $("btnLoad");
  const alertEl = $("alert");
  const tbody = $("tbody");
  const returnUrlEl = $("returnUrl"); // import.html içine hidden input olarak konmuş olmalı

  function normalizeList(resp) {
    // Start with shared normalizer if available
    const base = H && H.normalizeList ? H.normalizeList(resp) : null;
    if (Array.isArray(base) && base.length) return base;

    // local fallbacks for extra shapes (customers-import is used with multiple upstream payloads)
    if (Array.isArray(resp)) return resp;
    if (resp && Array.isArray(resp.rows)) return resp.rows;
    if (resp && Array.isArray(resp.Items)) return resp.Items;
    if (resp && Array.isArray(resp.items)) return resp.items;
    if (resp && Array.isArray(resp.Data)) return resp.Data;
    if (resp && resp.data && Array.isArray(resp.data)) return resp.data;

    // As a last resort, return the first array-like value found in the object.
    if (resp && typeof resp === "object") {
      for (const k of Object.keys(resp)) {
        if (Array.isArray(resp[k])) return resp[k];
        if (resp[k] && typeof resp[k] === "object" && Array.isArray(resp[k].items)) return resp[k].items;
        if (resp[k] && typeof resp[k] === "object" && Array.isArray(resp[k].Items)) return resp[k].Items;
      }
    }
    return [];
  }


  function pickCode(r) {
    return (
      r.customerCode ||
      r.CustomerCode ||
      r.code ||
      r.Code ||
      r.ARP_CODE ||
      r.arpCode ||
      ""
    ).toString().trim();
  }

  function pickName(r) {
    return (
      r.customerName ||
      r.CustomerName ||
      r.name ||
      r.Name ||
      r.title ||
      r.Title ||
      r.ARP_NAME ||
      r.arpName ||
      r.DEFINITION_ ||
      r.definition ||
      ""
    ).toString().trim();
  }

  function setAlert(type, msg) {
    if (!alertEl) return;
    alertEl.style.display = "block";
    alertEl.className = "alert " + (type === "error" ? "alert-error" : "alert-success");
    alertEl.textContent = msg;
  }

  function clearAlert() {
    if (!alertEl) return;
    alertEl.style.display = "none";
    alertEl.textContent = "";
    alertEl.className = "alert";
  }

  function escapeHtml(s) {
    return (s || "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function render(list) {
    tbody.innerHTML = "";

    const q = (qEl && qEl.value ? qEl.value : "").trim().toLowerCase();

    let filtered = list
      .map((r) => {
        const code = pickCode(r);
        const name = pickName(r) || code; // ✅ name fallback
        return { code, name };
      })
      .filter((x) => x.code); // ✅ code boşsa satır gösterme

    // If server-side search isn't supported, do a lightweight client-side filter.
    if (q) {
      filtered = filtered.filter((x) =>
        x.code.toLowerCase().includes(q) || x.name.toLowerCase().includes(q)
      );
    }

    if (filtered.length === 0) {
      tbody.innerHTML = `<tr><td colspan="3" class="muted">Kayıt bulunamadı.</td></tr>`;
      return;
    }

    for (const row of filtered) {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${escapeHtml(row.code)}</td>
        <td>${escapeHtml(row.name)}</td>
        <td style="text-align:right">
          <button class="btn" data-import="${escapeHtml(row.code)}" data-name="${escapeHtml(row.name)}">İçe Aktar</button>
        </td>
      `;
      tbody.appendChild(tr);
    }
  }

  async function load() {
    clearAlert();
    const q = (qEl && qEl.value ? qEl.value : "").trim();
    const limit = limitEl && limitEl.value ? limitEl.value : "20";

    const url = new URL("/api/arps", window.location.origin);
    if (q) url.searchParams.set("q", q);
    if (limit) url.searchParams.set("limit", limit);

    try {
      const resp = await fetch(url.toString(), { headers: { Accept: "application/json" } });
      if (!resp.ok) {
        const txt = await resp.text();
        throw new Error(`Logo’dan getir başarısız (${resp.status}): ${txt?.slice(0, 200) || ""}`);
      }
      const json = await resp.json();
      render(normalizeList(json));
    } catch (e) {
      setAlert("error", e.message || "Logo’dan getirilemedi. Loglara bakın.");
      render([]);
    }
  }

  async function doImport(code, name) {
    clearAlert();

    const payload = {
      customerCode: code,
      customerName: (name && name.trim()) ? name.trim() : code, // ✅ name fallback
      logoRef: code
    };

    try {
      const resp = await fetch("/api/customers/import-from-logo", {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify(payload),
      });

      const text = await resp.text();
      let data = null;
      try { data = text ? JSON.parse(text) : null; } catch (_) {}

      if (!resp.ok) {
        const msg = (data && (data.message || data.error)) ? (data.message || data.error) : (text || "");
        throw new Error(`İçe aktarma başarısız (${resp.status}): ${msg}`);
      }

      setAlert("success", "Müşteri içe aktarıldı.");

      const returnUrl = returnUrlEl ? (returnUrlEl.value || "").trim() : "";
      if (returnUrl) {
        const back = new URL(returnUrl, window.location.origin);
        back.searchParams.set("importedCustomerCode", code);
        window.location.href = back.toString();
        return;
      }
    } catch (e) {
      setAlert("error", e.message || "İçe aktarma başarısız. Loglara bakın.");
    }
  }

  btnLoad && btnLoad.addEventListener("click", (e) => {
    e.preventDefault();
    load();
  });

  tbody && tbody.addEventListener("click", (ev) => {
    const target = (ev.target && ev.target.nodeType === 1)
      ? ev.target
      : (ev.target && ev.target.parentElement ? ev.target.parentElement : null);
    const btn = target ? target.closest("button[data-import]") : null;
    if (!btn) return;
    const code = (btn.getAttribute("data-import") || "").trim();
    const name = (btn.getAttribute("data-name") || "").trim();
    if (!code) return;
    doImport(code, name);
  });

  // first load
  load();
})();
