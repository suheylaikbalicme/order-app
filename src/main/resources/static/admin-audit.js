// Admin Audit Log UI helpers (no deps)
(function () {
  function $(sel, root) { return (root || document).querySelector(sel); }
  function $all(sel, root) { return Array.from((root || document).querySelectorAll(sel)); }

  function decodeHtml(str) {
    if (str == null) return '';
    // Thymeleaf HTML-escapes attribute values; decode for JSON pretty print
    const txt = document.createElement('textarea');
    txt.innerHTML = String(str);
    return txt.value;
  }

  function prettyJson(raw) {
    const s = decodeHtml(raw).trim();
    if (!s) return '';
    try {
      const obj = JSON.parse(s);
      return JSON.stringify(obj, null, 2);
    } catch (e) {
      return s;
    }
  }

  function openModal(title, msg, meta) {
    const modal = $('#auditModal');
    const titleEl = $('#auditModalTitle');
    const msgEl = $('#auditModalMsg');
    const preEl = $('#auditModalPre');

    titleEl.textContent = title || 'Detay';
    msgEl.textContent = msg || '';
    preEl.textContent = prettyJson(meta) || '—';
    modal.style.display = 'flex';
  }

  function closeModal() {
    const modal = $('#auditModal');
    modal.style.display = 'none';
  }

  function init() {
    const closeBtn = $('#auditModalClose');
    const modal = $('#auditModal');
    if (closeBtn) closeBtn.addEventListener('click', closeModal);
    if (modal) modal.addEventListener('click', (e) => {
      if (e.target === modal) closeModal();
    });
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') closeModal();
    });

    $all('#auditTable button[data-id]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const id = btn.getAttribute('data-id');
        const msg = btn.getAttribute('data-msg');
        const meta = btn.getAttribute('data-meta');
        openModal('Audit #' + id, decodeHtml(msg), meta);
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
