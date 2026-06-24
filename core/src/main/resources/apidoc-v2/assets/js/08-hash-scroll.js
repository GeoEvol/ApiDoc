  // Intercept hash link clicks to scroll the window
  if (contentEl) {
    contentEl.addEventListener("click", function (event) {
      var link = event.target.closest("a[href^='#']");
      if (!link) return;
      var id = decodeURIComponent(link.getAttribute("href").slice(1));
      var target = document.getElementById(id);
      if (!target) return;
      event.preventDefault();
      window.scrollTo({ top: target.getBoundingClientRect().top + window.scrollY - 40, behavior: "smooth" });
    });
    // Handle initial URL hash on page load
    if (location.hash) {
      var hashTarget = document.getElementById(decodeURIComponent(location.hash.slice(1)));
      if (hashTarget) {
        setTimeout(function () {
          window.scrollTo({ top: hashTarget.getBoundingClientRect().top + window.scrollY - 40, behavior: "instant" });
        }, 100);
      }
    }
  }
