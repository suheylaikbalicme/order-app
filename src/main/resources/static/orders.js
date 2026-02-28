let orderItems = [];
let orderCart = [];

const $ = (id) => document.getElementById(id);

function setMsg(text) {
  const el = $("msg");
  if (!el) return;
  const t = String(text ?? "");
  el.textContent = t;
  el.style.display = t ? "block" : "none";
}

const h = (window.MrCrm && window.MrCrm.h) ? window.MrCrm.h : null;
if (!h) { throw new Error("MrCrm helpers not loaded"); }
const { money, num, intOrNull, normalizeList, pickCodeName, computeTotals } = h;

async function loadLogoItems() {
  const r = await fetch("/api/items?offset=0&limit=50&sort=Code");
  const j = await r.json().catch(() => ({}));
  orderItems = normalizeList(j).map(pickCodeName);

  const sel = $("itemSelect");
  sel.innerHTML = "";
  sel.appendChild(new Option("Stok seçiniz...", ""));
  orderItems.forEach((i) => {
    const opt = document.createElement("option");
    opt.value = i.code;
    opt.textContent = `${i.code} - ${i.name}`;
    opt.dataset.name = i.name;
    sel.appendChild(opt);
  });
}

function lockForm(locked) {
  const blocks = document.querySelectorAll("[data-requires-customer]");
  blocks.forEach((b) => {
    b.classList.toggle("disabled", locked);
    b.querySelectorAll("input, select, textarea, button").forEach((el) => {
      if (el.dataset.alwaysEnabled === "true") return;
      el.disabled = locked;
    });
  });
}

function currentCustomer() {
  const sel = $("customerSelect");
  const code = sel.value;
  const ds = sel.options[sel.selectedIndex]?.dataset || {};
  const name = ds.name || "";
  const phone = ds.phone || "";
  const email = ds.email || "";
  const address = ds.address || "";
  return { code, name, phone, email, address };
}

function renderCustomerCard() {
  const { code, name, phone, email, address } = currentCustomer();
  $("custCode").textContent = code || "-";
  $("custName").textContent = name || "-";
  const elPhone = $("custPhone");
  if (elPhone) elPhone.textContent = phone || "-";
  const elEmail = $("custEmail");
  if (elEmail) elEmail.textContent = email || "-";
  const elAddr = $("custAddress");
  if (elAddr) elAddr.textContent = address || "-";
}

function lineBase(it) {
  return num(it.quantity) * num(it.unitPrice);
}

function lineAfterDiscount(it) {
  const base = lineBase(it);
  const d = num(it.discountRate) / 100;
  return base * (1 - d);
}

function lineVat(it) {
  const net = lineAfterDiscount(it);
  const v = num(it.vatRate) / 100;
  return net * v;
}

function lineGross(it) {
  return lineAfterDiscount(it) + lineVat(it);
}

function calcTotals() {
  const t = computeTotals(orderCart);
$("subTotal").textContent = money(t.subtotal);
$("discountTotal").textContent = money(t.discount);
$("vatTotal").textContent = money(t.vat);
$("grandTotal").textContent = money(t.grand);
}

function renderTable() {
  const tbody = $("itemsTable").querySelector("tbody");
  tbody.innerHTML = "";

  orderCart.forEach((it, idx) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${it.itemCode}</td>
      <td>${it.itemName}</td>
      <td><input class="input sm" type="number" step="0.001" value="${it.quantity}"></td>
      <td><input class="input sm" type="number" step="0.01" value="${it.unitPrice}"></td>
      <td><input class="input sm" type="number" step="0.01" value="${it.discountRate}"></td>
      <td><input class="input sm" type="number" step="0.01" value="${it.vatRate}"></td>
      <td class="right"><span class="badge">${money(lineGross(it))}</span></td>
      <td class="right"><button class="btn danger" type="button">Sil</button></td>
    `;

    const [qtyEl, priceEl, discEl, vatEl] = tr.querySelectorAll("input");
    const recalc = () => {
      it.quantity = num(qtyEl.value);
      it.unitPrice = num(priceEl.value);
      it.discountRate = num(discEl.value);
      it.vatRate = num(vatEl.value);
      tr.querySelector("span").textContent = money(lineGross(it));
      calcTotals();
    };
    qtyEl.addEventListener("input", recalc);
    priceEl.addEventListener("input", recalc);
    discEl.addEventListener("input", recalc);
    vatEl.addEventListener("input", recalc);

    tr.querySelector("button").addEventListener("click", () => {
      orderCart.splice(idx, 1);
      renderTable();
      calcTotals();
    });

    tbody.appendChild(tr);
  });
}

function addLineFromInputs() {
  const sel = $("itemSelect");
  const itemCode = sel.value;
  const itemName = sel.options[sel.selectedIndex]?.dataset?.name || "";
  const quantity = num($("qty").value);
  const unitPrice = num($("unitPrice").value);
  const discountRate = num($("itemDiscount").value);
  const vatRate = num($("itemVat").value);

  if (!itemCode) {
    setMsg("Lütfen stok seçiniz.");
    return;
  }
  if (quantity <= 0) {
    setMsg("Miktar 0’dan büyük olmalı.");
    return;
  }

  orderCart.push({ itemCode, itemName, quantity, unitPrice, discountRate, vatRate });
  setMsg("");
  renderTable();
  calcTotals();

  $("itemSelect").value = "";
  $("qty").value = "1";
  $("unitPrice").value = "0";
  $("itemDiscount").value = "0";
  $("itemVat").value = "20";
}

async function saveOrder(action) {
  if (orderCart.length === 0) {
    setMsg("En az 1 kalem eklemelisin.");
    return;
  }

  const { code: customerCode, name: customerName } = currentCustomer();
  if (!customerCode) {
    setMsg("Lütfen müşteri seçiniz.");
    return;
  }

  const payload = {
    customerCode,
    customerName,
    orderDate: $("orderDate").value || null,
    currency: $("currency").value || null,
    exchangeRate: num($("exchangeRate").value),
    note: $("note").value || null,
    action,
    items: orderCart.map((it) => ({
      itemCode: it.itemCode,
      itemName: it.itemName,
      quantity: it.quantity,
      unitPrice: it.unitPrice,
      discountRate: it.discountRate,
      vatRate: it.vatRate,
    })),
  };

  setMsg("Kaydediliyor...");

  const r = await fetch("/api/orders", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!r.ok) {
    const txt = await r.text();
    setMsg(`Hata: ${r.status} ${txt}`);
    return;
  }

  const j = await r.json();
  window.location.href = `/orders/${j.id}`;
}

document.addEventListener("DOMContentLoaded", async () => {
  lockForm(true);

  // varsayılan tarih
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  $("orderDate").value = `${yyyy}-${mm}-${dd}`;

  $("customerSelect").addEventListener("change", () => {
    const has = !!$("customerSelect").value;
    lockForm(!has);
    renderCustomerCard();
  });

  // currency -> fx
  const currencyEl = $("currency");
  const rateEl = $("exchangeRate");
  const fillRate = async () => {
    const cur = (currencyEl.value || "").toUpperCase();
    if (!cur || cur === "TRY") {
      rateEl.value = "1";
      return;
    }
    try {
      const r = await fetch(`/api/fx/rate?base=TRY&currency=${encodeURIComponent(cur)}`);
      if (!r.ok) throw new Error(String(r.status));
      const j = await r.json();
      rateEl.value = String(j.rate ?? "");
    } catch (e) {
      setMsg("Kur otomatik alınamadı. Manuel girebilirsiniz.");
    }
  };
  currencyEl.addEventListener("change", fillRate);
  fillRate();

  try {
    await loadLogoItems();
  } catch (e) {
    setMsg("Stok listesi çekilemedi. Logo token sorunu olabilir.");
  }

  $("addItemBtn").addEventListener("click", addLineFromInputs);
  $("saveDraftBtn").addEventListener("click", () => saveOrder("draft"));
  $("submitBtn").addEventListener("click", () => saveOrder("submit"));

  renderCustomerCard();
  renderTable();
  calcTotals();
});
