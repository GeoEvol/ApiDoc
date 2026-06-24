  if (toggle && nav) {
    toggle.addEventListener("click", function () {
      var open = document.body.classList.toggle("ad-nav-open");
      toggle.setAttribute("aria-expanded", open ? "true" : "false");
      if (open) scheduleCurrentNavSync({ force: true });
    });

    nav.addEventListener("click", function (event) {
      var link = event.target.closest("a");
      if (!link) return;
      if (link.closest(".is-platform-disabled")) return;
      scheduleCurrentNavSync({ force: true });
      if (window.matchMedia("(max-width: 760px)").matches) {
        document.body.classList.remove("ad-nav-open");
        toggle.setAttribute("aria-expanded", "false");
      }
    });
  }

  var bookToggle = document.querySelector(".ad-book-nav-toggle");
  var contentEl = document.getElementById("main-content");

  function currentScrollTop() {
    return window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0;
  }

  function maxScrollTop() {
    return Math.max(0, document.documentElement.scrollHeight - document.documentElement.clientHeight);
  }

  function applyCollapsed(state) {
    document.body.classList.toggle("ad-nav-collapsed", state);
    var label = state ? "Show navigation" : "Hide navigation";
    if (bookToggle) {
      bookToggle.setAttribute("aria-expanded", state ? "false" : "true");
      bookToggle.setAttribute("aria-label", label);
      bookToggle.setAttribute("data-title", label);
    }
  }

  function toggleBookNav() {
    var scrollable = maxScrollTop();
    var ratio = scrollable > 0 ? currentScrollTop() / scrollable : 0;
    var nextCollapsed = !document.body.classList.contains("ad-nav-collapsed");
    applyCollapsed(nextCollapsed);
    window.requestAnimationFrame(function () {
      var nextScrollable = maxScrollTop();
      window.scrollTo({ top: Math.round(ratio * Math.max(0, nextScrollable)), behavior: "instant" });
      if (!nextCollapsed) scheduleCurrentNavSync({ force: true });
    });
  }

  applyCollapsed(false);
  if (bookToggle) {
    bookToggle.addEventListener("click", toggleBookNav);
  }

  if (packagesRoot) {
    packagesRoot.open = localStorage.getItem(packagesExpandedStorageKey) !== "false";
    packagesRoot.addEventListener("toggle", function () {
      localStorage.setItem(packagesExpandedStorageKey, packagesRoot.open ? "true" : "false");
    });
  }
