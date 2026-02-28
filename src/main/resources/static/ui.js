(() => {
  const modal = document.getElementById("confirmModal");
  if (!modal) return;

  const overlay = modal.querySelector(".modal-overlay");
  const titleEl = modal.querySelector("[data-modal-title]");
  const msgEl = modal.querySelector("[data-modal-message]");
  const btnCancel = modal.querySelector("[data-modal-cancel]");
  const btnOk = modal.querySelector("[data-modal-ok]");

  let pendingAction = null;

  const openModal = (title, message, onOk) => {
    titleEl.textContent = title || "Onay";
    msgEl.textContent = message || "Emin misiniz?";
    pendingAction = onOk;

    modal.classList.add("open");
    btnOk.focus();
  };

  const closeModal = () => {
    modal.classList.remove("open");
    pendingAction = null;
  };

  btnCancel.addEventListener("click", (e) => {
    e.preventDefault();
    closeModal();
  });

  btnOk.addEventListener("click", (e) => {
    e.preventDefault();
    if (pendingAction) pendingAction();
    closeModal();
  });

  overlay?.addEventListener("click", closeModal);

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && modal.classList.contains("open")) closeModal();
  });

  document.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-confirm]");
    if (!btn) return;

    const msg = btn.getAttribute("data-confirm");
    if (!msg) return;

    // Buton bir form submit butonuyse
    const form = btn.closest("form");

    e.preventDefault();

    openModal("Onay", msg, () => {
      if (form) {
        // form submit
        if (typeof form.requestSubmit === "function") {
          form.requestSubmit(btn);
        } else {
          form.submit();
        }
      } else {
        // link ise href'e git
        const href = btn.getAttribute("href");
        if (href) window.location.href = href;
      }
    });
  });
})();

// Dropdown UX: click-to-toggle + outside click closes
(() => {
  const dropdowns = Array.from(document.querySelectorAll(".dropdown"));
  if (dropdowns.length === 0) return;

  // Mark body so CSS can prefer .dropdown.open over :hover (prevents gap/flicker)
  document.body?.classList.add("js");

  const closeAll = () => dropdowns.forEach((d) => d.classList.remove("open"));

  dropdowns.forEach((d) => {
    const btn = d.querySelector("button");
    const menu = d.querySelector(".dropdown-menu");
    if (!btn || !menu) return;

    btn.addEventListener("click", (e) => {
      e.preventDefault();
      e.stopPropagation();

      const willOpen = !d.classList.contains("open");
      closeAll();
      if (willOpen) d.classList.add("open");
    });

    // Click inside menu should not close immediately (links will navigate anyway)
    menu.addEventListener("click", (e) => e.stopPropagation());
  });

  document.addEventListener("click", () => closeAll());
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closeAll();
  });
})();
