(function () {
  if (window.__apidocDevsiteLoaded) return;
  window.__apidocDevsiteLoaded = true;

  var toggle = document.querySelector(".ad-devsite-nav-toggle");
  var nav = document.getElementById("ad-book-nav");
  var platformSelect = document.getElementById("ad-platform-select");
  var platformStorageKey = "apidoc.platform";
  var navCollapsedStorageKey = "apidoc.navCollapsed";
  if (toggle && nav) {
    toggle.addEventListener("click", function () {
      var open = document.body.classList.toggle("ad-nav-open");
      toggle.setAttribute("aria-expanded", open ? "true" : "false");
    });

    nav.addEventListener("click", function (event) {
      if (event.target.closest("a") && window.matchMedia("(max-width: 760px)").matches) {
        document.body.classList.remove("ad-nav-open");
        toggle.setAttribute("aria-expanded", "false");
      }
    });
  }

  var bookToggle = document.querySelector(".ad-book-nav-toggle");
  if (bookToggle) {
    var storedCollapsed = localStorage.getItem(navCollapsedStorageKey) === "true";
    document.body.classList.toggle("ad-nav-collapsed", storedCollapsed);
    bookToggle.addEventListener("click", function () {
      var collapsed = document.body.classList.toggle("ad-nav-collapsed");
      localStorage.setItem(navCollapsedStorageKey, collapsed ? "true" : "false");
    });
  }

  var current = document.querySelector(".ad-devsite-book-nav a[href='" + location.pathname.split("/").pop() + "']");
  if (current) {
    current.classList.add("is-current");
  }

  document.addEventListener("click", function (event) {
    var button = event.target.closest(".ad-copy-anchor");
    if (!button) return;
    var anchor = button.getAttribute("data-anchor");
    if (!anchor) return;
    var value = location.href.replace(/#.*$/, "") + "#" + anchor;
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(value).catch(function () {});
    }
  });

  document.addEventListener("click", function (event) {
    var button = event.target.closest(".ad-copy-code");
    if (!button) return;
    var block = button.closest("pre");
    var code = block ? block.querySelector("code") : null;
    var value = code ? code.innerText.replace(/\s+$/g, "") : "";
    if (!value) return;
    var icon = button.querySelector("img");
    var previous = icon ? icon.getAttribute("src") : "";
    var checked = previous.replace(/copy\.svg$/, "checked.svg");
    function markCopied() {
      button.classList.add("is-copied");
      if (icon && checked !== previous) icon.setAttribute("src", checked);
      window.setTimeout(function () {
        button.classList.remove("is-copied");
        if (icon && previous) icon.setAttribute("src", previous);
      }, 1200);
    }
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(value).then(markCopied).catch(function () {});
    } else {
      markCopied();
    }
  });

  function currentPlatform() {
    return platformSelect ? platformSelect.value || "all" : (localStorage.getItem(platformStorageKey) || "all");
  }

  function supportsPlatform(el, platform) {
    if (platform === "all") return true;
    var raw = el.getAttribute("data-platforms");
    if (!raw || raw.trim() === "") return true;
    return raw.split(/\s+/).indexOf(platform) !== -1;
  }

  function setPlatformHidden(el, hidden) {
    el.classList.toggle("ad-platform-hidden", hidden);
  }

  function applyPlatformFilter() {
    var platform = currentPlatform();
    if (platformSelect) {
      localStorage.setItem(platformStorageKey, platform);
    }
    Array.prototype.forEach.call(document.querySelectorAll("[data-platforms]"), function (el) {
      setPlatformHidden(el, !supportsPlatform(el, platform));
    });
    applyNavFilter();
    window.dispatchEvent(new CustomEvent("apidoc-platform-change", { detail: { platform: platform } }));
  }

  var navFilter = document.getElementById("ad-nav-filter");
  function applyNavFilter() {
    if (navFilter && nav) {
      var query = navFilter.value.trim().toLowerCase();
      Array.prototype.forEach.call(nav.querySelectorAll(".ad-package"), function (pkg) {
        if (!supportsPlatform(pkg, currentPlatform())) {
          pkg.hidden = true;
          return;
        }
        var packageText = (pkg.querySelector(".ad-package-name") || pkg).textContent.toLowerCase();
        var visibleTypes = 0;
        Array.prototype.forEach.call(pkg.querySelectorAll(".ad-book-overview, .ad-book-type"), function (link) {
          var text = (link.getAttribute("data-filter-text") || link.textContent || "").toLowerCase();
          var visible = supportsPlatform(link, currentPlatform()) && (!query || text.indexOf(query) !== -1 || packageText.indexOf(query) !== -1);
          link.hidden = !visible;
          if (visible) visibleTypes++;
        });
        Array.prototype.forEach.call(pkg.querySelectorAll(".ad-package-group"), function (group) {
          var next = group.nextElementSibling;
          var hasVisibleType = false;
          while (next && !next.classList.contains("ad-package-group")) {
            if (next.matches && next.matches(".ad-book-type") && !next.hidden && supportsPlatform(next, currentPlatform())) {
              hasVisibleType = true;
              break;
            }
            next = next.nextElementSibling;
          }
          group.hidden = !hasVisibleType || !supportsPlatform(group, currentPlatform());
        });
        var packageVisible = !query || packageText.indexOf(query) !== -1 || visibleTypes > 0;
        pkg.hidden = !packageVisible || visibleTypes === 0;
        if (query && packageVisible) pkg.open = true;
      });
    }
  }

  if (platformSelect) {
    var storedPlatform = localStorage.getItem(platformStorageKey) || "all";
    if (Array.prototype.some.call(platformSelect.options, function (option) { return option.value === storedPlatform; })) {
      platformSelect.value = storedPlatform;
    }
    platformSelect.addEventListener("change", applyPlatformFilter);
  }

  if (navFilter && nav) {
    navFilter.addEventListener("input", applyNavFilter);
  }
  applyPlatformFilter();

  document.addEventListener("keydown", function (event) {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
      var search = document.getElementById("ad-search");
      if (search) {
        event.preventDefault();
        search.focus();
        search.select();
      }
    }
    if (event.key === "Escape" && document.body.classList.contains("ad-nav-open")) {
      document.body.classList.remove("ad-nav-open");
      if (toggle) toggle.setAttribute("aria-expanded", "false");
    }
  });

  var tocLinks = Array.prototype.slice.call(document.querySelectorAll(".ad-devsite-toc a[href^='#']"));
  if (tocLinks.length) {
    var tocTargets = tocLinks.map(function (link) {
      var id = decodeURIComponent(link.getAttribute("href").slice(1));
      return { link: link, target: document.getElementById(id) };
    }).filter(function (item) {
      return item.target;
    });
    var tocTicking = false;
    function setActiveToc() {
      tocTicking = false;
      if (!tocTargets.length) return;
      var active = tocTargets[0];
      var anchorLine = Math.max(90, Math.floor(window.innerHeight * 0.22));
      tocTargets.forEach(function (item) {
        if (item.target.getBoundingClientRect().top <= anchorLine) active = item;
      });
      tocLinks.forEach(function (link) { link.classList.remove("is-active"); });
      active.link.classList.add("is-active");
      if (active.link.scrollIntoView) {
        active.link.scrollIntoView({ block: "nearest" });
      }
    }
    function requestTocSync() {
      if (tocTicking) return;
      tocTicking = true;
      window.requestAnimationFrame(setActiveToc);
    }
    window.addEventListener("scroll", requestTocSync, { passive: true });
    window.addEventListener("resize", requestTocSync);
    setActiveToc();
  }
})();
