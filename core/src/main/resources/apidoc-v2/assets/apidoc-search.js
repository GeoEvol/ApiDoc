(function () {
  var input = document.getElementById("ad-search");
  var panel = document.getElementById("ad-search-results");
  if (!input || !panel) return;

  var script = document.currentScript;
  var src = script && script.src || location.href;
  var rootPrefix = script && script.getAttribute("data-root-prefix") || "";
  var searchIndex = script && script.getAttribute("data-search-index") || "";
  var url = searchIndex ? new URL(searchIndex, location.href) : new URL("../search-index.json", src);
  var items = [];
  var activeIndex = -1;
  var lastHits = [];
  var KIND_WEIGHTS = {
    PACKAGE: 120,
    CLASS: 110,
    INTERFACE: 110,
    ENUM: 108,
    ANNOTATION: 106,
    RECORD: 104,
    EXCEPTION: 102,
    ERROR: 102,
    CONSTRUCTOR: 90,
    METHOD: 80,
    CONSTANT: 70,
    FIELD: 60
  };

  input.setAttribute("aria-controls", panel.id || "ad-search-results");
  input.setAttribute("aria-expanded", "false");
  panel.setAttribute("role", "listbox");

  function esc(value) {
    return String(value || "").replace(/[&<>"]/g, function (char) {
      return {
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "\"": "&quot;"
      }[char];
    });
  }

  function matchRanges(value, query) {
    var text = String(value || "");
    var needle = normalize(query);
    if (!needle) return [];
    var lower = text.toLowerCase();
    var ranges = [];
    var from = 0;
    var index = lower.indexOf(needle, from);
    while (index !== -1) {
      ranges.push([index, index + needle.length]);
      from = index + needle.length;
      index = lower.indexOf(needle, from);
    }
    return ranges;
  }

  function renderHighlightedText(value, query) {
    var text = String(value || "");
    var ranges = matchRanges(text, query);
    if (!ranges.length) return esc(text);
    var html = "";
    var from = 0;
    ranges.forEach(function (range) {
      html += esc(text.substring(from, range[0]));
      html += "<mark class=\"ad-search-highlight\">" + esc(text.substring(range[0], range[1])) + "</mark>";
      from = range[1];
    });
    return html + esc(text.substring(from));
  }

  function snippetFor(item, query) {
    var candidates = [item.summary, item.qualifiedName, item.displaySignature, item.searchText];
    for (var i = 0; i < candidates.length; i += 1) {
      var text = String(candidates[i] || "").trim();
      var index = text.toLowerCase().indexOf(normalize(query));
      if (index < 0) continue;
      var start = Math.max(0, index - 45);
      var end = Math.min(text.length, index + String(query || "").length + 75);
      return (start > 0 ? "\u2026" : "") + text.substring(start, end) + (end < text.length ? "\u2026" : "");
    }
    return "";
  }

  function renderSnippet(item, query, title, subtitle) {
    if (matchRanges(title, query).length || matchRanges(subtitle, query).length) return "";
    var snippet = snippetFor(item, query);
    return snippet ? "<span class=\"ad-search-snippet\">" + renderHighlightedText(snippet, query) + "</span>" : "";
  }

  function normalizedQuery() {
    return input.value.trim().toLowerCase();
  }

  function normalize(value) {
    return String(value || "").trim().toLowerCase();
  }

  function currentPlatform() {
    return localStorage.getItem("apidoc.platform") || "all";
  }

  function matchesPlatform(item) {
    var platform = currentPlatform();
    if (platform === "all") return true;
    var platforms = item.platforms || [];
    if (!platforms || !platforms.length) return true;
    return platforms.indexOf(platform) !== -1;
  }

  function bestFieldScore(values, query, checks) {
    var best = 0;
    values.forEach(function (value) {
      var normalized = normalize(value);
      if (!normalized) return;
      checks.forEach(function (check) {
        if (check.match(normalized, query)) {
          best = Math.max(best, check.score);
        }
      });
    });
    return best;
  }

  function segments(value) {
    return normalize(value).split(/[.#\s()[\],<>]+/).filter(function (segment) {
      return !!segment;
    });
  }

  function segmentScore(value, query) {
    var best = 0;
    segments(value).forEach(function (segment) {
      if (segment === query) {
        best = Math.max(best, 900);
      } else if (segment.indexOf(query) === 0) {
        best = Math.max(best, 780);
      }
    });
    return best;
  }

  function isSignatureQuery(query) {
    return /[\s()[\],<>]/.test(normalize(query));
  }

  function scoreItem(item, query) {
    query = normalize(query);
    if (!query) return 0;
    var score = 0;
    score += bestFieldScore([item.label, item.simpleName], query, [
      { score: 1200, match: function (value, needle) { return value === needle; } },
      { score: 850, match: function (value, needle) { return value.indexOf(needle) === 0; } },
      { score: 460, match: function (value, needle) { return value.indexOf(needle) >= 0; } }
    ]);
    score += Math.max(
      bestFieldScore([item.qualifiedName], query, [
        { score: 1100, match: function (value, needle) { return value === needle; } },
        { score: 420, match: function (value, needle) { return value.indexOf(needle) >= 0; } }
      ]),
      segmentScore(item.qualifiedName, query)
    );
    if (isSignatureQuery(query)) {
      score += bestFieldScore([item.displaySignature], query, [
        { score: 1050, match: function (value, needle) { return value === needle; } },
        { score: 740, match: function (value, needle) { return value.indexOf(needle) === 0; } },
        { score: 360, match: function (value, needle) { return value.indexOf(needle) >= 0; } }
      ]);
    }
    score += bestFieldScore(item.tokens || [], query, [
      { score: 650, match: function (value, needle) { return value === needle; } },
      { score: 540, match: function (value, needle) { return value.indexOf(needle) === 0; } },
      { score: 300, match: function (value, needle) { return value.indexOf(needle) >= 0; } }
    ]);
    score += Math.max(
      bestFieldScore([item.ownerSimpleName], query, [
        { score: 620, match: function (value, needle) { return value === needle; } },
        { score: 520, match: function (value, needle) { return value.indexOf(needle) === 0; } }
      ]),
      bestFieldScore([item.packageName], query, [
        { score: 260, match: function (value, needle) { return value.indexOf(needle) >= 0; } }
      ])
    );
    score += Math.max(
      bestFieldScore([item.summary], query, [
        { score: 220, match: function (value, needle) { return value.indexOf(needle) >= 0; } }
      ]),
      bestFieldScore([item.searchText], query, [
        { score: 160, match: function (value, needle) { return value.indexOf(needle) >= 0; } }
      ])
    );
    return score;
  }

  function compareText(left, right) {
    left = normalize(left);
    right = normalize(right);
    if (left < right) return -1;
    if (left > right) return 1;
    return 0;
  }

  function sortSearchResults(items, query) {
    return items.map(function (item) {
      return {
        item: item,
        score: scoreItem(item, query),
        kindWeight: KIND_WEIGHTS[item.kind] || 0
      };
    }).filter(function (result) {
      return result.score > 0;
    }).sort(function (left, right) {
      return (right.score - left.score)
        || (right.kindWeight - left.kindWeight)
        || compareText(left.item.label, right.item.label)
        || compareText(left.item.ownerQualifiedName, right.item.ownerQualifiedName)
        || compareText(left.item.qualifiedName, right.item.qualifiedName)
        || compareText(left.item.displaySignature, right.item.displaySignature)
        || compareText(left.item.url, right.item.url);
    }).map(function (result) {
      return result.item;
    });
  }

  function kindLabel(kind) {
    return String(kind || "").toLowerCase().replace(/(^|_)([a-z])/g, function (_, prefix, char) {
      return (prefix ? " " : "") + char.toUpperCase();
    });
  }

  function resultSubtitle(item) {
    return [kindLabel(item.kind), item.ownerSimpleName, item.packageName].filter(function (value) {
      return !!value;
    }).join(" \u00b7 ");
  }

  function hasUrlScheme(value) {
    return /^[a-z][a-z0-9+.-]*:/i.test(value);
  }

  function shouldPrefixUrl(value) {
    return value && !hasUrlScheme(value) && value.indexOf("//") !== 0 && value.charAt(0) !== "/" && value.charAt(0) !== "#";
  }

  function targetUrl(item) {
    var itemUrl = item.url || "";
    if (item.anchor && itemUrl.indexOf("#") < 0) {
      itemUrl += "#" + item.anchor;
    }
    return shouldPrefixUrl(itemUrl) ? rootPrefix + itemUrl : itemUrl;
  }

  function updateActive() {
    var links = panel.querySelectorAll("a.ad-search-result");
    Array.prototype.forEach.call(links, function (link, index) {
      if (index === activeIndex) {
        link.classList.add("is-active");
        link.focus();
      } else {
        link.classList.remove("is-active");
      }
    });
  }

  function render(query) {
    query = query.trim().toLowerCase();
    window.dispatchEvent(new CustomEvent("apidoc-search-query-change", { detail: { query: query } }));
    if (!query) {
      panel.classList.remove("open");
      panel.innerHTML = "";
      input.setAttribute("aria-expanded", "false");
      lastHits = [];
      activeIndex = -1;
      return;
    }
    var hits = sortSearchResults(items.filter(function (item) {
      return matchesPlatform(item);
    }), query).slice(0, 30);
    lastHits = hits;
    activeIndex = -1;

    panel.innerHTML = hits.length ? hits.map(function (item) {
      var title = item.displayTitle || item.simpleName || item.label || item.qualifiedName;
      var subtitle = resultSubtitle(item);
      return "<a class=\"ad-search-result\" href=\"" + esc(targetUrl(item)) + "\">"
        + "<strong>" + renderHighlightedText(title, query) + "</strong>"
        + "<span>" + renderHighlightedText(subtitle, query) + "</span>"
        + renderSnippet(item, query, title, subtitle)
        + "</a>";
    }).join("") : "<div class=\"ad-search-result\"><strong>No results</strong></div>";
    panel.classList.add("open");
    input.setAttribute("aria-expanded", "true");
  }

  fetch(url)
    .then(function (response) { return response.ok ? response.json() : []; })
    .then(function (data) { items = Array.isArray(data) ? data : []; })
    .catch(function () { items = []; });

  input.addEventListener("input", function () { render(input.value); });
  input.addEventListener("focus", function () {
      if (normalizedQuery()) {
         render(input.value);
      }
    });
  window.addEventListener("apidoc-platform-change", function () { render(normalizedQuery()); });
  function onSearchKeydown(event) {
    if (!panel.classList.contains("open") || !lastHits.length) return;
    if (event.key === "ArrowDown") {
      event.preventDefault();
      activeIndex = Math.min(activeIndex + 1, lastHits.length - 1);
      updateActive();
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      activeIndex = Math.max(activeIndex - 1, 0);
      updateActive();
    } else if (event.key === "Enter" && activeIndex >= 0) {
      event.preventDefault();
      location.href = targetUrl(lastHits[activeIndex]);
    } else if (event.key === "Escape") {
      panel.classList.remove("open");
      input.setAttribute("aria-expanded", "false");
      input.focus();
    }
  }
  input.addEventListener("keydown", onSearchKeydown);
  panel.addEventListener("keydown", onSearchKeydown);
  window.__APIDOC_SEARCH_TESTING__ = {
    scoreItem: scoreItem,
    sortSearchResults: sortSearchResults,
    matchesPlatform: matchesPlatform,
    matchRanges: matchRanges,
    renderHighlightedText: renderHighlightedText,
    snippetFor: snippetFor
  };
  document.addEventListener("click", function (event) {
    if (!panel.contains(event.target) && event.target !== input) {
      panel.classList.remove("open");
      input.setAttribute("aria-expanded", "false");
    }
  });
})();
