(function () {
  const overlay = document.getElementById("confirmOverlay");
  const message = document.getElementById("confirmMessage");
  const cancelButton = document.getElementById("confirmCancelButton");
  const submitButton = document.getElementById("confirmSubmitButton");
  let pendingForm = null;

  if (!overlay || !message || !cancelButton || !submitButton) {
    return;
  }

  function closeModal() {
    overlay.hidden = true;
    pendingForm = null;
  }

  document.addEventListener("click", function (event) {
    const button = event.target.closest("[data-confirm]");
    if (!button) {
      return;
    }

    const form = button.closest("form");
    if (!form) {
      return;
    }

    event.preventDefault();
    pendingForm = form;
    message.textContent = button.getAttribute("data-confirm") || "Bạn có chắc muốn tiếp tục?";
    overlay.hidden = false;
  });

  cancelButton.addEventListener("click", closeModal);
  overlay.addEventListener("click", function (event) {
    if (event.target === overlay) {
      closeModal();
    }
  });

  submitButton.addEventListener("click", function () {
    if (pendingForm) {
      pendingForm.submit();
    }
    closeModal();
  });
})();
