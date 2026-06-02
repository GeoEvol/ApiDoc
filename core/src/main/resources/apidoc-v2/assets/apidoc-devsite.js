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
  var restoreHandle = document.querySelector(".ad-book-nav-restore-handle");
  var bookToggleLabel = bookToggle ? bookToggle.querySelector(".ad-book-nav-toggle-label") : null;
  function applyCollapsed(state) {
    document.body.classList.toggle("ad-nav-collapsed", state);
    var label = state ? "Show navigation" : "Hide navigation";
    if (bookToggle) {
      bookToggle.setAttribute("aria-expanded", state ? "false" : "true");
      bookToggle.setAttribute("aria-label", label);
    }
    if (restoreHandle) restoreHandle.setAttribute("aria-expanded", state ? "false" : "true");
    if (bookToggleLabel) bookToggleLabel.textContent = label;
  }
  var storedCollapsed = localStorage.getItem(navCollapsedStorageKey) === "true";
  applyCollapsed(storedCollapsed);
  if (bookToggle) {
    bookToggle.addEventListener("click", function () {
      applyCollapsed(true);
      localStorage.setItem(navCollapsedStorageKey, "true");
    });
  }
  if (restoreHandle) {
    restoreHandle.addEventListener("click", function () {
      applyCollapsed(false);
      localStorage.setItem(navCollapsedStorageKey, "false");
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

  function setPlatformDisabled(el, disabled) {
    if (!el) return;
    el.classList.toggle("is-platform-disabled", disabled);
    if (disabled) {
      el.setAttribute("aria-disabled", "true");
      if (el.matches && el.matches("a")) {
        el.setAttribute("tabindex", "-1");
      }
    } else {
      el.removeAttribute("aria-disabled");
      if (el.matches && el.matches("a")) {
        el.removeAttribute("tabindex");
      }
    }
  }

  function hasSupportedTypeLink(container, platform, directOnly) {
    var selector = directOnly ? ":scope > .ad-book-type" : ".ad-book-type";
    return Array.prototype.some.call(container.querySelectorAll(selector), function (link) {
      return supportsPlatform(link, platform);
    });
  }

  function applyNavPlatformState(platform) {
    if (!nav) return;
    Array.prototype.forEach.call(nav.querySelectorAll(".ad-package"), function (pkg) {
      var packageSupported = hasSupportedTypeLink(pkg, platform, false);
      setPlatformDisabled(pkg, !packageSupported);
      setPlatformDisabled(pkg.querySelector(":scope > summary"), !packageSupported);

      var overview = pkg.querySelector(":scope > .ad-book-overview");
      setPlatformDisabled(overview, !packageSupported);

      Array.prototype.forEach.call(pkg.querySelectorAll(":scope > .ad-package-group"), function (group) {
        var groupSupported = hasSupportedTypeLink(group, platform, true);
        setPlatformDisabled(group, !groupSupported);
        setPlatformDisabled(group.querySelector(":scope > summary"), !groupSupported);
        Array.prototype.forEach.call(group.querySelectorAll(":scope > .ad-book-type"), function (link) {
          setPlatformDisabled(link, !supportsPlatform(link, platform));
        });
      });
    });
  }

  function applyPlatformFilter() {
    var platform = currentPlatform();
    if (platformSelect) {
      localStorage.setItem(platformStorageKey, platform);
    }
    Array.prototype.forEach.call(document.querySelectorAll("[data-platforms]"), function (el) {
      if (nav && nav.contains(el)) return;
      setPlatformDisabled(el, !supportsPlatform(el, platform));
    });
    applyNavPlatformState(platform);
    window.dispatchEvent(new CustomEvent("apidoc-platform-change", { detail: { platform: platform } }));
  }

  if (platformSelect) {
    var storedPlatform = localStorage.getItem(platformStorageKey) || "all";
    if (Array.prototype.some.call(platformSelect.options, function (option) { return option.value === storedPlatform; })) {
      platformSelect.value = storedPlatform;
    }
    platformSelect.addEventListener("change", applyPlatformFilter);
  }

  document.addEventListener("click", function (event) {
    var link = event.target.closest("a");
    if (!link || !link.closest(".is-platform-disabled")) return;
    event.preventDefault();
    event.stopPropagation();
  });

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
