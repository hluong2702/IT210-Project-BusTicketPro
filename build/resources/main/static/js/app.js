
'use strict';

// ── Toast system ──────────────────────────────────────────────
const Toast = (() => {
  let container;
  const getContainer = () => {
    if (!container) {
      container = document.createElement('div');
      container.className = 'toast-container';
      document.body.appendChild(container);
    }
    return container;
  };

  const show = (message, type = 'info', duration = 4000) => {
    const icons = { success: '✅', error: '❌', info: 'ℹ️', warning: '⚠️' };
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
      <span class="toast-icon">${icons[type] || '💬'}</span>
      <span class="toast-text">${message}</span>`;
    getContainer().appendChild(toast);
    setTimeout(() => {
      toast.classList.add('fade-out');
      setTimeout(() => toast.remove(), 400);
    }, duration);
    return toast;
  };

  return { show, success: (m) => show(m,'success'), error: (m) => show(m,'error'), info: (m) => show(m,'info') };
})();

// ── Modal system ──────────────────────────────────────────────
const Modal = (() => {
  const open = (id) => {
    const el = document.getElementById(id);
    if (el) { el.classList.add('active'); document.body.style.overflow = 'hidden'; }
  };
  const close = (id) => {
    const el = document.getElementById(id);
    if (el) { el.classList.remove('active'); document.body.style.overflow = ''; }
  };
  const closeAll = () => {
    document.querySelectorAll('.modal-overlay.active').forEach(m => {
      m.classList.remove('active');
    });
    document.body.style.overflow = '';
  };
  return { open, close, closeAll };
})();

// ── API helper ────────────────────────────────────────────────
const API = (() => {
  const csrfToken = () => document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = () => document.querySelector('meta[name="_csrf_header"]')?.content;

  const request = async (url, options = {}) => {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    const token = csrfToken();
    const header = csrfHeader();
    if (token && header) headers[header] = token;

    const res = await fetch(url, { ...options, headers });
    const data = await res.json();
    return { ok: res.ok, data };
  };

  return {
    get:    (url)         => request(url, { method: 'GET' }),
    post:   (url, body)   => request(url, { method: 'POST',   body: JSON.stringify(body) }),
    put:    (url, body)   => request(url, { method: 'PUT',    body: JSON.stringify(body) }),
    delete: (url)         => request(url, { method: 'DELETE' }),
  };
})();

// ── Sidebar toggle ────────────────────────────────────────────
function initSidebar() {
  const toggle = document.getElementById('sidebarToggle');
  const sidebar = document.querySelector('.sidebar');
  const backdrop = document.getElementById('sidebarBackdrop');
  if (!toggle || !sidebar) return;

  const open  = () => { sidebar.classList.add('open'); backdrop?.classList.add('active'); };
  const close = () => { sidebar.classList.remove('open'); backdrop?.classList.remove('active'); };

  toggle.addEventListener('click', () => sidebar.classList.contains('open') ? close() : open());
  backdrop?.addEventListener('click', close);
}

// ── Confirm dialog helper ─────────────────────────────────────
function confirmAction(message, onConfirm) {
  const overlay = document.getElementById('confirmModal');
  if (!overlay) { if (confirm(message)) onConfirm(); return; }
  document.getElementById('confirmMessage').textContent = message;
  document.getElementById('confirmOk').onclick = () => { Modal.close('confirmModal'); onConfirm(); };
  Modal.open('confirmModal');
}

// ── Format currency ───────────────────────────────────────────
function formatVND(amount) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

// ── Format duration ───────────────────────────────────────────
function formatDuration(minutes) {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h > 0 ? `${h}h${m > 0 ? m + 'm' : ''}` : `${m}m`;
}

// ── Ticket status badge ───────────────────────────────────────
function statusBadge(status) {
  const map = {
    PENDING:   { cls: 'badge-pending',   label: '⏳ Chờ TT' },
    PAID:      { cls: 'badge-paid',      label: '✅ Đã TT' },
    CANCELLED: { cls: 'badge-cancelled', label: '❌ Đã hủy' },
  };
  const s = map[status] || { cls: 'badge-info', label: status };
  return `<span class="badge ${s.cls}">${s.label}</span>`;
}

// ── Auto-dismiss flash alerts ─────────────────────────────────
function initAlerts() {
  document.querySelectorAll('.alert[data-auto-dismiss]').forEach(el => {
    const ms = parseInt(el.dataset.autoDismiss) || 5000;
    setTimeout(() => el.style.display = 'none', ms);
  });
  document.querySelectorAll('.alert .close').forEach(btn => {
    btn.addEventListener('click', () => btn.closest('.alert').remove());
  });
}

// ── Modal close on backdrop click ────────────────────────────
function initModals() {
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', e => {
      if (e.target === overlay) Modal.closeAll();
    });
  });
  document.querySelectorAll('[data-modal-open]').forEach(btn => {
    btn.addEventListener('click', () => Modal.open(btn.dataset.modalOpen));
  });
  document.querySelectorAll('[data-modal-close]').forEach(btn => {
    btn.addEventListener('click', () => Modal.close(btn.dataset.modalClose));
  });
}

// ── Tables: client-side search ────────────────────────────────
function initTableSearch() {
  document.querySelectorAll('[data-table-search]').forEach(input => {
    const tableId = input.dataset.tableSearch;
    const table = document.getElementById(tableId);
    if (!table) return;
    input.addEventListener('input', () => {
      const q = input.value.toLowerCase();
      table.querySelectorAll('tbody tr').forEach(row => {
        row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
      });
    });
  });
}

// ── Ticket actions ────────────────────────────────────────────
function cancelTicket(ticketId, url, isPassenger = false) {
  const msg = isPassenger
    ? 'Bạn có chắc muốn hủy vé này không? Thao tác không thể hoàn tác.'
    : 'Xác nhận hủy vé này?';
  confirmAction(msg, async () => {
    const { ok, data } = await API.post(url, { reason: 'Nhân viên hủy' });
    if (ok && data.success) {
      Toast.success(data.message || 'Hủy vé thành công');
      setTimeout(() => location.reload(), 1200);
    } else {
      Toast.error(data.message || 'Có lỗi xảy ra');
    }
  });
}

function confirmPayment(ticketId, url) {
  confirmAction('Xác nhận thanh toán cho vé này?', async () => {
    const { ok, data } = await API.post(url, {});
    if (ok && data.success) {
      Toast.success(data.message || 'Xác nhận thành công');
      setTimeout(() => location.reload(), 1200);
    } else {
      Toast.error(data.message || 'Có lỗi xảy ra');
    }
  });
}

function deleteItem(url, msg = 'Bạn có chắc muốn xóa?') {
  confirmAction(msg, async () => {
    const { ok, data } = await API.post(url, {});
    if (ok && data.success) {
      Toast.success(data.message || 'Xóa thành công');
      setTimeout(() => location.reload(), 1000);
    } else {
      Toast.error(data.message || 'Không thể xóa');
    }
  });
}

// ── Seat map ──────────────────────────────────────────────────
let selectedSeatId   = null;
let selectedSeatNum  = null;

function initSeatMap() {
  document.querySelectorAll('.seat.available').forEach(seat => {
    seat.addEventListener('click', () => selectSeat(seat));
  });
}

function selectSeat(el) {
  document.querySelectorAll('.seat.selected').forEach(s => {
    s.classList.remove('selected');
    s.classList.add('available');
  });

  el.classList.remove('available');
  el.classList.add('selected');

  selectedSeatId  = el.dataset.seatId;
  selectedSeatNum = el.dataset.seatNum;

  const seatInput = document.getElementById('selectedSeatId');
  if (seatInput) {
    seatInput.value = selectedSeatId;
  }

  const display = document.getElementById('selectedSeatDisplay');
  if (display) {
    display.textContent = 'Ghế ' + selectedSeatNum;
  }

  document.getElementById('bookBtn')?.removeAttribute('disabled');
  document.getElementById('seatSelectionInfo')?.classList.remove('d-none');
  document.getElementById('noSeatWarning')?.classList.add('d-none');
}

// ── Ticket lookup (public) ────────────────────────────────────
function initTicketLookup() {
  const form = document.getElementById('lookupForm');
  if (!form) return;
  form.addEventListener('submit', async e => {
    e.preventDefault();
    const code  = document.getElementById('lookupCode').value.trim();
    const phone = document.getElementById('lookupPhone').value.trim();
    if (!code || !phone) { Toast.error('Vui lòng nhập đầy đủ thông tin'); return; }

    const btn = form.querySelector('[type=submit]');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span> Đang tra cứu...';

    const { ok, data } = await API.get(`/ticket/search?code=${encodeURIComponent(code)}&phone=${encodeURIComponent(phone)}`);
    btn.disabled = false;
    btn.innerHTML = '🔍 Tra cứu';

    const resultEl = document.getElementById('lookupResult');
    if (ok && data.success) {
      const t = data.data;
      resultEl.innerHTML = buildTicketResult(t);
      resultEl.classList.remove('d-none');
    } else {
      resultEl.innerHTML = `<div class="alert alert-danger">❌ ${data.message || 'Không tìm thấy vé'}</div>`;
      resultEl.classList.remove('d-none');
    }
  });
}

function buildTicketResult(t) {
  const statusBadgeHtml = {
    PENDING:   '<span class="badge badge-pending">⏳ Chờ thanh toán</span>',
    PAID:      '<span class="badge badge-paid">✅ Đã thanh toán</span>',
    CANCELLED: '<span class="badge badge-cancelled">❌ Đã hủy</span>',
  }[t.status] || '';

  return `
    <div class="ticket-card">
      <div class="ticket-header">
        <div>
          <div style="font-size:.75rem;opacity:.7">MÃ VÉ</div>
          <div class="ticket-code">${t.ticketCode}</div>
        </div>
        ${statusBadgeHtml}
      </div>
      <div class="ticket-body">
        <div class="ticket-route mb-4">
          <div class="ticket-city"><div class="ticket-city">${t.departureName}</div></div>
          <div class="ticket-arrow">✈️ ──────── </div>
          <div class="ticket-city">${t.arrivalName}</div>
        </div>
        <div class="ticket-info-grid">
          <div class="ticket-info-item"><div class="label">Hành khách</div><div class="value">${t.passengerName}</div></div>
          <div class="ticket-info-item"><div class="label">SĐT</div><div class="value">${t.passengerPhone}</div></div>
          <div class="ticket-info-item"><div class="label">Ghế</div><div class="value">${t.seatNumber}${t.seatFloor > 1 ? ' (Tầng '+t.seatFloor+')' : ''}</div></div>
          <div class="ticket-info-item"><div class="label">Giờ đi</div><div class="value">${formatDateTime(t.departureTime)}</div></div>
          <div class="ticket-info-item"><div class="label">Xe</div><div class="value">${t.licensePlate}</div></div>
          <div class="ticket-info-item"><div class="label">Giá vé</div><div class="value" style="color:var(--primary)">${formatVND(t.totalAmount)}</div></div>
        </div>
      </div>
    </div>`;
}

function formatDateTime(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' });
}

// ── Change password modal ─────────────────────────────────────
function initChangePassword() {
  const form = document.getElementById('changePasswordForm');
  if (!form) return;
  form.addEventListener('submit', async e => {
    e.preventDefault();
    const oldPw = document.getElementById('oldPassword').value;
    const newPw = document.getElementById('newPassword').value;
    const cfPw  = document.getElementById('confirmPassword').value;
    if (newPw !== cfPw) { Toast.error('Mật khẩu mới không khớp'); return; }
    if (newPw.length < 6) { Toast.error('Mật khẩu phải ít nhất 6 ký tự'); return; }

    const { ok, data } = await API.post('/passenger/change-password', { oldPassword: oldPw, newPassword: newPw });
    if (ok && data.success) {
      Toast.success(data.message);
      Modal.close('changePasswordModal');
      form.reset();
    } else {
      Toast.error(data.message || 'Đổi mật khẩu thất bại');
    }
  });
}

// ── Init on DOMContentLoaded ──────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  initSidebar();
  initAlerts();
  initModals();
  initTableSearch();
  initSeatMap();
  initTicketLookup();
  initChangePassword();

  // Show flash toasts from Thymeleaf attributes
  const flashSuccess = document.querySelector('[data-flash-success]')?.dataset.flashSuccess;
  const flashError   = document.querySelector('[data-flash-error]')?.dataset.flashError;
  if (flashSuccess) Toast.success(flashSuccess);
  if (flashError)   Toast.error(flashError);

  // Keyboard: Escape to close modals
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') Modal.closeAll();
  });
});
