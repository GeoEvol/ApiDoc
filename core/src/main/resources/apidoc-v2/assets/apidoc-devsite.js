(function () {
  if (window.__apidocDevsiteLoaded) return;
  window.__apidocDevsiteLoaded = true;

  var toggle = document.querySelector(".ad-devsite-nav-toggle");
  var nav = document.getElementById("ad-book-nav");
  var packagesRoot = document.getElementById("ad-packages-root");
  var platformSelect = document.getElementById("ad-platform-select");
  var platformStorageKey = "apidoc.platform";
  var packagesExpandedStorageKey = "apidoc.packagesExpanded";
  var currentSearchQuery = "";
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
  var contentEl = document.getElementById("main-content");
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
    var scrollable = document.documentElement.scrollHeight - document.documentElement.clientHeight;
    var ratio = scrollable > 0 ? window.scrollY / scrollable : 0;
    applyCollapsed(!document.body.classList.contains("ad-nav-collapsed"));
    window.requestAnimationFrame(function () {
      var nextScrollable = document.documentElement.scrollHeight - document.documentElement.clientHeight;
      window.scrollTo({ top: Math.round(ratio * Math.max(0, nextScrollable)), behavior: "instant" });
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

  var current = document.querySelector(".ad-devsite-book-nav a[href$='" + location.pathname.split("/").pop() + "']");
  if (current) {
    current.classList.add("is-current");
    var parent = current.parentElement;
    while (parent) {
      if (parent.tagName === "DETAILS") {
        parent.open = true;
      }
      parent = parent.parentElement;
    }
  }

  document.addEventListener("click", function (event) {
    var button = event.target.closest(".ad-copy-anchor");
    if (!button) return;
    var anchor = button.getAttribute("data-anchor");
    if (!anchor) return;
    var value = location.href.replace(/#.*$/, "") + "#" + anchor;
    var icon = button.querySelector("img");
    var previous = icon ? icon.getAttribute("src") : "";
    var checked = previous.replace(/link\.svg$/, "checked.svg");
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

  function matchesNavQuery(el, query) {
    if (!el || !query) return false;
    var text = el.getAttribute("data-filter-text") || el.textContent || "";
    return text.toLowerCase().indexOf(query) !== -1;
  }

  function appendWbr(parent) {
    parent.appendChild(document.createElement("wbr"));
  }

  function appendSegmentWithPeriodicBreaks(parent, text) {
    var value = String(text || "");
    if (!value) return;
    var interval = 12;
    for (var i = 0; i < value.length; i += interval) {
      if (i > 0) appendWbr(parent);
      parent.appendChild(document.createTextNode(value.substring(i, i + interval)));
    }
  }

  function appendBreaksAfter(parent, text, delimiter) {
    var value = String(text || "");
    var from = 0;
    for (var i = 0; i < value.length; i++) {
      if (value.charAt(i) === delimiter) {
        appendSegmentWithPeriodicBreaks(parent, value.substring(from, i + 1));
        appendWbr(parent);
        from = i + 1;
      }
    }
    appendSegmentWithPeriodicBreaks(parent, value.substring(from));
  }

  function appendCamelBreaks(parent, text) {
    var value = String(text || "");
    var from = 0;
    for (var i = 1; i < value.length; i++) {
      if (/[A-Z]/.test(value.charAt(i))) {
        appendSegmentWithPeriodicBreaks(parent, value.substring(from, i));
        appendWbr(parent);
        from = i;
      }
    }
    appendSegmentWithPeriodicBreaks(parent, value.substring(from));
  }

  function appendTextWithSemanticBreaks(parent, text) {
    var value = String(text || "");
    if (!value) return;
    if (value.length < 8) {
      parent.appendChild(document.createTextNode(value));
      return;
    }
    if (value.indexOf(".") !== -1) {
      appendBreaksAfter(parent, value, ".");
    } else if (value.indexOf("_") !== -1) {
      appendBreaksAfter(parent, value, "_");
    } else if (value.indexOf("-") !== -1) {
      appendBreaksAfter(parent, value, "-");
    } else {
      appendCamelBreaks(parent, value);
    }
  }

  function renderNavLabel(label, query) {
    var text = label.getAttribute("data-nav-label") || label.textContent || "";
    label.textContent = "";
    if (!query) {
      appendTextWithSemanticBreaks(label, text);
      return;
    }
    var lower = text.toLowerCase();
    var from = 0;
    var index = lower.indexOf(query, from);
    while (index !== -1) {
      if (index > from) appendTextWithSemanticBreaks(label, text.substring(from, index));
      var mark = document.createElement("mark");
      mark.className = "ad-search-highlight";
      mark.textContent = text.substring(index, index + query.length);
      label.appendChild(mark);
      from = index + query.length;
      index = lower.indexOf(query, from);
    }
    if (from < text.length) appendTextWithSemanticBreaks(label, text.substring(from));
  }

  function highlightNavLabels(query) {
    if (!nav) return;
    Array.prototype.forEach.call(nav.querySelectorAll("[data-nav-label]"), function (label) {
      renderNavLabel(label, query);
    });
  }

  function applyNavSearch(query) {
    currentSearchQuery = String(query || "").trim().toLowerCase();
    if (!nav) return;

    var filtering = !!currentSearchQuery;
    var platform = currentPlatform();
    var rootSummary = packagesRoot ? packagesRoot.querySelector(":scope > summary") : null;
    var rootMatch = filtering && matchesNavQuery(rootSummary, currentSearchQuery);
    var filterDescendants = filtering && !rootMatch;
    highlightNavLabels(currentSearchQuery);

    if (filtering && packagesRoot) {
      packagesRoot.open = true;
      localStorage.setItem(packagesExpandedStorageKey, "true");
    }

    Array.prototype.forEach.call(nav.querySelectorAll(".ad-book-index-link"), function (link) {
      link.hidden = filterDescendants && !matchesNavQuery(link, currentSearchQuery);
    });

    Array.prototype.forEach.call(nav.querySelectorAll(".ad-package"), function (pkg) {
      var packageSummary = pkg.querySelector(":scope > summary");
      var packageSupported = hasSupportedTypeLink(pkg, platform, false);
      var packageMatch = matchesNavQuery(packageSummary, currentSearchQuery);
      var visibleDescendant = false;
      var overview = pkg.querySelector(":scope > .ad-book-overview");

      if (overview) {
        var overviewVisible = supportsPlatform(overview, platform)
          && (!filterDescendants || packageMatch || matchesNavQuery(overview, currentSearchQuery));
        overview.hidden = filterDescendants && !overviewVisible;
        visibleDescendant = visibleDescendant || overviewVisible;
      }

      Array.prototype.forEach.call(pkg.querySelectorAll(":scope > .ad-package-group"), function (group) {
        var groupSummary = group.querySelector(":scope > summary");
        var groupSupported = hasSupportedTypeLink(group, platform, true);
        var groupMatch = packageMatch || matchesNavQuery(groupSummary, currentSearchQuery);
        var visibleType = false;

        Array.prototype.forEach.call(group.querySelectorAll(":scope > .ad-book-type"), function (link) {
          var linkVisible = supportsPlatform(link, platform)
            && (!filterDescendants || groupMatch || matchesNavQuery(link, currentSearchQuery));
          link.hidden = filterDescendants && !linkVisible;
          visibleType = visibleType || linkVisible;
        });

        var groupVisible = groupSupported && (!filterDescendants || groupMatch || visibleType);
        group.hidden = filterDescendants && !groupVisible;
        if (filtering && groupVisible) group.open = true;
        visibleDescendant = visibleDescendant || groupVisible;
      });

      var packageVisible = packageSupported && (!filterDescendants || packageMatch || visibleDescendant);
      pkg.hidden = filterDescendants && !packageVisible;
      if (filtering && packageVisible) pkg.open = true;
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
    applyNavSearch(currentSearchQuery);
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

  window.addEventListener("apidoc-search-query-change", function (event) {
    applyNavSearch(event.detail && event.detail.query || "");
  });

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
  var contentEl = document.getElementById("main-content");
  if (tocLinks.length && contentEl) {
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
})();
