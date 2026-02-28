let orderItems = [];
let cart = [];

const $ = (id) => document.getElementById(id);
const h = (window.MrCrm && window.MrCrm.h) ? window.MrCrm.h : null;
if (!h) { throw new Error("MrCrm helpers not loaded"); }
const { money, num, intOrNull, normalizeList, pickCodeName, computeTotals, computeLineTotal } = h;

function setMsg(text, isError = true) {
  const el = $("msg");
  if (!el) return;
  const t = String(text || "").trim();
  if (!t) {
    el.style.display = "none";
    el.textContent = "";
    return;
  }
  el.style.display = "block";
  el.textContent = t;
  el.classList.toggle("error", !!isError);
}

function markInvalid(el, title) {
  if (!el) return;
  el.classList.add("is-invalid");
  el.setAttribute("aria-invalid", "true");
  if (title) el.setAttribute("title", title);
}

function clearInvalid(el) {
  if (!el) return;
  el.classList.remove("is-invalid");
  el.removeAttribute("aria-invalid");
  el.removeAttribute("title");
}

function scrollToEl(el) {
  if (!el || !el.scrollIntoView) return;
  try {
    el.scrollIntoView({ behavior: "smooth", block: "center" });
  } catch (_) {
    el.scrollIntoView();
  }
}


// Server-side doldurulmuş müşteri listesini bozmamak için
// sadece boşsa API denemesi yapıyoruz (legacy fallback).
async function loadCustomers() {
  const sel = $("customerSelect");
  if (sel && sel.options && sel.options.length > 1) return;

  sel.innerHTML = "";
  sel.appendChild(new Option("Bir müşteri seçiniz...", ""));
  const r = await fetch("/api/customers/list").catch(() => null);
  if (!r || !r.ok) return;
  const j = await r.json().catch(() => []);
  (Array.isArray(j) ? j : []).forEach((c) => {
    const opt = document.createElement("option");
    opt.value = c.customerCode;
    opt.textContent = `${c.customerCode} - ${c.customerName}`;
    opt.dataset.name = c.customerName;
    sel.appendChild(opt);
  });
}

async function loadItems() {
  const sel = $("itemSelect");
  sel.innerHTML = "";
  sel.appendChild(new Option("Stok seçiniz...", ""));
  const r = await fetch("/api/items?offset=0&limit=200&sort=Code");
  const j = await r.json().catch(() => ({}));
  orderItems = normalizeList(j).map(pickCodeName);
  orderItems.forEach((i) => {
    const opt = document.createElement("option");
    opt.value = i.code;
    opt.textContent = `${i.code} - ${i.name}`;
    opt.dataset.name = i.name;
    sel.appendChild(opt);
  });
}

function renderTable() {
  const tb = document.querySelector("#itemsTable tbody");
  tb.innerHTML = "";
  cart.forEach((row, idx) => {
    const tr = document.createElement("tr");
    const calcLineGross = () => {
      const gross = num(row.qty) * num(row.unitPrice);
      const after = gross * (1 - num(row.discountRate) / 100);
      const v = after * (num(row.vatRate) / 100);
      return after + v;
    };

    tr.innerHTML = `
      <td>${row.itemCode}</td>
      <td>${row.itemName || ""}</td>
      <td><input class="input sm" type="number" step="0.001" value="${row.qty}"></td>
      <td><input class="input sm" type="number" step="0.01" value="${row.unitPrice}"></td>
      <td><input class="input sm" type="number" step="0.01" value="${row.discountRate}"></td>
      <td><input class="input sm" type="number" step="0.01" value="${row.vatRate}"></td>
      <td class="right"><span class="badge">${money(calcLineGross())}</span></td>
      <td class="right"><button class="btn ghost" data-i="${idx}">Sil</button></td>
    `;
    tb.appendChild(tr);

    const inputs = tr.querySelectorAll("input");
    const [qtyEl, priceEl, discEl, vatEl] = inputs;
    const recalc = () => {
      row.qty = num(qtyEl.value);
      row.unitPrice = num(priceEl.value);
      row.discountRate = num(discEl.value);
      row.vatRate = num(vatEl.value);
      tr.querySelector("span").textContent = money(calcLineGross());
      renderTotals();
    };
    qtyEl.addEventListener("input", recalc);
    priceEl.addEventListener("input", recalc);
    discEl.addEventListener("input", recalc);
    vatEl.addEventListener("input", recalc);
  });

  tb.querySelectorAll("button[data-i]").forEach((b) => {
    b.addEventListener("click", () => {
      const i = parseInt(b.dataset.i, 10);
      cart.splice(i, 1);
      renderTable();
      renderTotals();
    });
  });
}

function renderTotals() {
  const t = computeTotals(cart);
  $("subTotal").textContent = money(t.subtotal);
  $("discountTotal").textContent = money(t.discount);
  $("vatTotal").textContent = money(t.vat);
  $("grandTotal").textContent = money(t.grand);
}

function addItem() {
  const sel = $("itemSelect");
  const code = sel.value;
  const name = sel.options[sel.selectedIndex]?.dataset?.name || "";
  if (!code) return setMsg("Stok seçiniz.");
  const qty = num($("qty").value);
  if (qty <= 0) return setMsg("Miktar 0 olamaz.");
  const unit = num($("unitPrice").value);
  const disc = num($("itemDiscount").value);
  const vat = num($("itemVat").value);

  cart.push({ itemCode: code, itemName: name, qty, unitPrice: unit, discountRate: disc, vatRate: vat });
  renderTable();
  renderTotals();
}

function buildPayload(action) {
  const customerSel = $("customerSelect");
  const customerCode = customerSel.value;
  const customerName = customerSel.options[customerSel.selectedIndex]?.dataset?.name || "";
  if (!customerCode) throw new Error("Müşteri seçiniz.");

  if (cart.length < 1) throw new Error("En az 1 kalem ekleyin.");

  return {
    customerCode,
    customerName,
    orderDate: $("orderDate").value || null,
    currency: $("currency").value || "TRY",
    exchangeRate: num($("exchangeRate").value) || 1,
    note: $("note").value || null,
    action,
    revisionReason: $("revisionReason").value || null,
    items: cart.map((r) => ({
      itemCode: r.itemCode,
      itemName: r.itemName,
      quantity: r.qty,
      unitPrice: r.unitPrice,
      discountRate: r.discountRate,
      vatRate: r.vatRate
    }))
  };
}

function setupFxRate() {
  const currencyEl = $("currency");
  const rateEl = $("exchangeRate");
  h.wireAutoFxRate({
    currencyEl,
    rateEl,
    base: "TRY",
    onRefresh: () => { if (window.__orderSummaryRefresh) { try { window.__orderSummaryRefresh(); } catch (_) {} } },
    onWarn: (m) => setMsg(m, true)
  });
}

async function loadExisting() {
  const id = $("orderId").value;
  const r = await fetch(`/api/orders/${id}`);
  const o = await r.json();
  // header
  $("orderDate").value = (o.orderDate || "").slice(0, 10);
  $("currency").value = o.currency || "TRY";
  $("exchangeRate").value = o.exchangeRate ?? 1;
  $("note").value = o.note ?? "";

  // customer
  const sel = $("customerSelect");
  if (sel) {
    [...sel.options].forEach((opt) => {
      if (opt.value === o.customerCode) opt.selected = true;
    });
  }

  // items
  cart = (o.items || []).map((it) => ({
    itemCode: it.itemCode,
    itemName: it.itemName,
    qty: Number(it.quantity || 0),
    unitPrice: Number(it.unitPrice || 0),
    discountRate: Number(it.discountRate || 0),
    vatRate: Number(it.vatRate || 20),
  }));
  renderTable();
  renderTotals();

  // status-based buttons
  const isDraft = (o.status || "") === "DRAFT";
  $("saveDraftBtn").style.display = isDraft ? "inline-block" : "none";
  $("submitBtn").style.display = isDraft ? "inline-block" : "none";
  $("reviseBtn").style.display = isDraft ? "none" : "inline-block";
  const resubmit = $("resubmitBtn");
  if (resubmit) resubmit.style.display = isDraft ? "none" : "inline-block";

  return o;
}

function ensureRevisionReason(action) {
  const reasonEl = $("revisionReason");
  const v = String(reasonEl?.value || "").trim();
  clearInvalid(reasonEl);
  if ((action === "revise" || action === "resubmit") && !v) {
    markInvalid(reasonEl, "Revizyon sebebi zorunludur");
    setMsg("Revizyon sebebi zorunludur.");
    scrollToEl(reasonEl);
    try { reasonEl.focus(); } catch (_) {}
    return false;
  }
  return true;
}

function wireReviseBannerActions() {
  const reasonEl = $("revisionReason");
  const btnRev = $("bannerReviseBtn");
  const btnRes = $("bannerResubmitBtn");
  const btnFocus = $("bannerFocusReasonBtn");

  const update = () => {
    const ok = String(reasonEl?.value || "").trim().length > 0;
    [btnRev, btnRes].forEach((b) => {
      if (!b) return;
      b.disabled = !ok;
      b.classList.toggle("disabled", !ok);
    });
  };

  if (btnFocus && reasonEl) {
    btnFocus.addEventListener("click", () => {
      clearInvalid(reasonEl);
      scrollToEl(reasonEl);
      try { reasonEl.focus(); } catch (_) {}
    });
  }

  if (btnRev) {
    btnRev.addEventListener("click", () => {
      if (!ensureRevisionReason("revise")) return;
      $("reviseBtn")?.click();
    });
  }

  if (btnRes) {
    btnRes.addEventListener("click", () => {
      if (!ensureRevisionReason("resubmit")) return;
      $("resubmitBtn")?.click();
    });
  }

  if (reasonEl) {
    reasonEl.addEventListener("input", () => {
      clearInvalid(reasonEl);
      update();
    });
  }

  update();
}

async function save(action) {
  try {
    setMsg("", false);
    const id = $("orderId").value;
    if (!ensureRevisionReason(action)) return;

    if (action === "resubmit") {
      const ok = window.confirm("Revizyon kaydedilecek ve sipariş tekrar onaya gönderilecek. Devam edilsin mi?");
      if (!ok) return;
    }
    const payload = buildPayload(action);
    const r = await fetch(`/api/orders/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    const j = await r.json().catch(() => ({}));
    if (!r.ok) {
      throw new Error(j.message || j.error || `HTTP ${r.status}`);
    }
    window.location.href = `/orders/${j.id}`;
  } catch (e) {
    setMsg(e.message || String(e));
  }
}

(async function init() {
  try {
    // URL params: /edit?mode=revise&reason=...
    const params = new URLSearchParams(window.location.search);
    const mode = (params.get("mode") || "").toLowerCase();
    const isReviseModeParam = mode === "revise";
    const reason = params.get("reason");
    if (reason && $("revisionReason")) $("revisionReason").value = reason;

    await loadItems();
    await loadCustomers();
    const existing = await loadExisting();
    setupFxRate();

    const isDraft = (existing?.status || "") === "DRAFT";
    const effectiveReviseMode = isReviseModeParam || !isDraft;

    if (effectiveReviseMode) {
      const banner = document.getElementById("reviseBanner");
      if (banner) banner.style.display = "block";

      if ($("saveDraftBtn")) $("saveDraftBtn").style.display = "none";
      if ($("submitBtn")) $("submitBtn").style.display = "none";
      if ($("reviseBtn")) $("reviseBtn").style.display = "inline-block";
      if ($("resubmitBtn")) $("resubmitBtn").style.display = "inline-block";

      wireReviseBannerActions();

      const reasonEl = $("revisionReason");
      if (reasonEl && !String(reasonEl.value || "").trim()) {
        scrollToEl(reasonEl);
        try { reasonEl.focus(); } catch (_) {}
      }
    }

    $("addItemBtn").addEventListener("click", addItem);
    $("saveDraftBtn").addEventListener("click", () => save("draft"));
    $("submitBtn").addEventListener("click", () => save("submit"));
    $("reviseBtn").addEventListener("click", () => save("revise"));
    const resubmit = $("resubmitBtn");
    if (resubmit) resubmit.addEventListener("click", () => save("resubmit"));

    const reasonEl = $("revisionReason");
    if (reasonEl) reasonEl.addEventListener("input", () => clearInvalid(reasonEl));
  } catch (e) {
    setMsg(e.message || String(e));
  }
})();