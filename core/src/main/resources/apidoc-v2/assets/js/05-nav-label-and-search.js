
  function matchesNavQuery(el, query) {
    if (!el || !query) return false;
    var text = el.getAttribute("data-filter-text") || el.textContent || "";
    return text.toLowerCase().indexOf(query) !== -1;
  }

  function appendWbr(parent) {
    parent.appendChild(document.createElement("wbr"));
  }

  function appendTextSegment(parent, text) {
    var value = String(text || "");
    if (!value) return;
    parent.appendChild(document.createTextNode(value));
  }

  function appendBreaksAfter(parent, text, delimiter) {
    var value = String(text || "");
    var from = 0;
    for (var i = 0; i < value.length; i++) {
      if (value.charAt(i) === delimiter) {
        appendTextSegment(parent, value.substring(from, i + 1));
        appendWbr(parent);
        from = i + 1;
      }
    }
    appendTextSegment(parent, value.substring(from));
  }

  function appendCamelBreaks(parent, text) {
    var value = String(text || "");
    var from = 0;
    for (var i = 1; i < value.length; i++) {
      if (/[A-Z]/.test(value.charAt(i))) {
        appendTextSegment(parent, value.substring(from, i));
        appendWbr(parent);
        from = i;
      }
    }
    appendTextSegment(parent, value.substring(from));
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
    scheduleCurrentNavSync();
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
  window.addEventListener("load", function () { scheduleCurrentNavSync(); });
  window.addEventListener("apidoc-content-updated", function () {
    navUserScrolled = false;
    scheduleCurrentNavSync({ force: true });
  });

  window.addEventListener("apidoc-search-query-change", function (event) {
    applyNavSearch(event.detail && event.detail.query || "");
  });
