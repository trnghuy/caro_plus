(function () {
  const page = document.querySelector("[data-auth-page]");
  if (!page) {
    return;
  }

  const tabs = Array.from(document.querySelectorAll("[data-auth-tab]"));
  const forms = Array.from(document.querySelectorAll("[data-auth-form]"));
  const initialView = page.dataset.initialView || "login";

  function show(view) {
    forms.forEach((form) => {
      form.hidden = form.dataset.authForm !== view;
    });

    tabs.forEach((tab) => {
      tab.classList.toggle("active", tab.dataset.authTab === view);
    });
  }

  tabs.forEach((tab) => {
    tab.addEventListener("click", function () {
      show(tab.dataset.authTab);
    });
  });

  const registerForm = document.getElementById("registerForm");
  if (registerForm) {
    registerForm.addEventListener("submit", function (event) {
      const password = document.getElementById("registerPassword");
      const confirm = document.getElementById("registerConfirm");
      const error = document.getElementById("registerError");

      if (!password || !confirm || !error) {
        return;
      }

      if (password.value !== confirm.value) {
        event.preventDefault();
        error.hidden = false;
        error.textContent = "Mật khẩu xác nhận không khớp.";
        confirm.focus();
        return;
      }

      error.hidden = true;
      error.textContent = "";
    });
  }

  show(initialView);
})();
