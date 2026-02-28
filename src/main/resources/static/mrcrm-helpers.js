
(function () {
  "use strict";

  const MrCrm = (window.MrCrm = window.MrCrm || {});
  const h = (MrCrm.h = MrCrm.h || {});

  h.money = function money(v) {
    const n = Number(v || 0);
    return Number.isFinite(n) ? n.toFixed(2) : "0.00";
  };

  h.num = function num(v) {
    const s = String(v ?? "").trim().replace(",", ".");
    const n = parseFloat(s);
    return Number.isFinite(n) ? n : 0;
  };

  // Returns integer or null. Useful for optional integer fields (validity days, payment days, etc.)
  h.intOrNull = function intOrNull(v) {
    const s = String(v ?? "").trim();
    if (!s) return null;
    const n = parseInt(s, 10);
    return Number.isFinite(n) ? n : null;
  };

  // Normalizes various API list response shapes into an array.
  h.normalizeList = function normalizeList(resp) {
    if (Array.isArray(resp)) return resp;
    if (resp && Array.isArray(resp.rows)) return resp.rows;
    if (resp && Array.isArray(resp.Items)) return resp.Items;
    if (resp && Array.isArray(resp.items)) return resp.items;
    if (resp && Array.isArray(resp.Data)) return resp.Data;
    return [];
  };

  // Tries to extract a {code, name} pair from various Logo/ERP payload shapes.
  h.pickCodeName = function pickCodeName(x) {
    const code = x?.code ?? x?.Code ?? x?.ItemCode ?? x?.ID ?? "";
    const name = x?.title ?? x?.Title ?? x?.description ?? x?.Description ?? x?.name ?? x?.Name ?? "";
    return { code: String(code), name: String(name) };
  };


  h.computeTotals = function computeTotals(rows) {
    const list = Array.isArray(rows) ? rows : [];
    let subtotal = 0, discount = 0, vat = 0, grand = 0;

    for (const r of list) {
      const qty = h.num(r.qty ?? r.quantity);
      const unit = h.num(r.unitPrice);
      const discRate = h.num(r.discountRate);
      const vatRate = h.num(r.vatRate);

      const gross = qty * unit;
      const after = gross * (1 - discRate / 100);
      const lineDisc = gross - after;
      const lineVat = after * (vatRate / 100);

      subtotal += gross;
      discount += lineDisc;
      vat += lineVat;
      grand += after + lineVat;
    }
    return { subtotal, discount, vat, grand };
  };

  h.computeLineTotal = function computeLineTotal(row) {
    const qty = h.num(row.qty ?? row.quantity);
    const unit = h.num(row.unitPrice);
    const discRate = h.num(row.discountRate);
    const vatRate = h.num(row.vatRate);

    const gross = qty * unit;
    const after = gross * (1 - discRate / 100);
    const lineVat = after * (vatRate / 100);
    return after + lineVat;
  };

  h.wireAutoFxRate = function wireAutoFxRate(opts) {
    const currencyEl = opts?.currencyEl;
    const rateEl = opts?.rateEl;
    const base = String(opts?.base || "TRY").toUpperCase();
    const onRefresh = typeof opts?.onRefresh === "function" ? opts.onRefresh : function(){};
    const onWarn = typeof opts?.onWarn === "function" ? opts.onWarn : function(){};

    if (!currencyEl || !rateEl) return;

    const fillRate = async () => {
      const cur = String(currencyEl.value || base).toUpperCase();

      if (!cur || cur === base) {
        rateEl.value = "1";
        try { onRefresh(); } catch (_) {}
        return;
      }

      try {
        const r = await fetch(`/api/fx/rate?base=${encodeURIComponent(base)}&currency=${encodeURIComponent(cur)}`);
        const j = await r.json().catch(() => ({}));
        rateEl.value = String(j.rate ?? "");
      } catch (_) {
        onWarn("Kur otomatik alınamadı. Manuel girebilirsiniz.");
      }

      try { onRefresh(); } catch (_) {}
    };

    currencyEl.addEventListener("change", fillRate);
  };
})();
