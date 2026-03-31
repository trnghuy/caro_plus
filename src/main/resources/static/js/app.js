(function () {
  function escapeHtml(value) {
    return String(value ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function initials(value) {
    const text = String(value ?? "").trim();
    if (!text) {
      return "CG";
    }

    const parts = text.split(/\s+/).slice(0, 2);
    return parts
      .map((part) => part.charAt(0).toUpperCase())
      .join("");
  }

  function formatDate(value) {
    if (!value) {
      return "";
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return "";
    }

    return date.toLocaleString("vi-VN");
  }

  function formatPoints(value) {
    const numeric = Number(value);
    if (Number.isNaN(numeric)) {
      return "0";
    }

    const rounded = Math.round(numeric * 2) / 2;
    return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(1);
  }

  function statusClass(value) {
    const status = String(value ?? "").toLowerCase();
    if (status === "playing") return "badge-playing";
    if (status === "full") return "badge-full";
    if (status === "ready") return "badge-ready";
    if (status === "online") return "badge-online";
    if (status === "danger") return "badge-danger";
    return "badge-waiting";
  }

  function statusLabel(value) {
    const status = String(value ?? "").toLowerCase();
    if (status === "waiting") return "Đang chờ";
    if (status === "playing") return "Đang chơi";
    if (status === "full") return "Đủ người";
    if (status === "ready") return "Sẵn sàng";
    if (status === "online") return "Online";
    return value || "Không rõ";
  }

  window.CaroApp = {
    escapeHtml,
    initials,
    formatDate,
    formatPoints,
    statusClass,
    statusLabel,
  };
})();
