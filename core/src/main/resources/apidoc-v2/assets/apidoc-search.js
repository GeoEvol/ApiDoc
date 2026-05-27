(function () {
  var input = document.getElementById("ad-search");
  var panel = document.getElementById("ad-search-results");
  if (!input || !panel) return;

  var src = document.currentScript && document.currentScript.src || location.href;
  var rootPrefix = document.currentScript && document.currentScript.getAttribute('data-root-prefix') || "";
  var url = new URL("../search-index.json", src);
  var items = [];
  var activeIndex = -1;
  var lastHits = [];
  var kindFilter = document.getElementById("ad-search-kind");

  input.setAttribute("aria-controls", panel.id || "ad-search-results");
  input.setAttribute("aria-expanded", "false");
  panel.setAttribute("role", "listbox");

  if (!kindFilter) {
    kindFilter = document.createElement("select");
    kindFilter.id = "ad-search-kind";
    kindFilter.className = "ad-search-kind";
    kindFilter.setAttribute("aria-label", "Search kind");
    kindFilter.innerHTML = [
      "<option value=\"\">All</option>",
      "<option value=\"PACKAGE\">Packages</option>",
      "<option value=\"CLASS\">Classes</option>",
      "<option value=\"INTERFACE\">Interfaces</option>",
      "<option value=\"ENUM\">Enums</option>",
      "<option value=\"ANNOTATION\">Annotations</option>",
      "<option value=\"RECORD\">Records</option>",
      "<option value=\"CONSTRUCTOR\">Constructors</option>",
      "<option value=\"METHOD\">Methods</option>",
      "<option value=\"FIELD\">Fields</option>",
      "<option value=\"CONSTANT\">Constants</option>"
    ].join("");
    input.parentNode.insertBefore(kindFilter, panel);
  }

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

  function normalizedQuery() {
    return input.value.trim().toLowerCase();
  }

  function matchesKind(item) {
    return !kindFilter.value || String(item.kind || "") === kindFilter.value;
  }

  function matchesLabel(item, query) {
    if (!query) return true;
    return [
      item.label,
      item.qualifiedName,
      item.packageName,
      item.ownerName,
      item.summary,
      item.kind,
      item.displaySignature
    ].concat(item.tokens || []).some(function (value) {
      return String(value || "").toLowerCase().indexOf(query) >= 0;
    });
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
    if (!query && !kindFilter.value) {
      panel.classList.remove("open");
      panel.innerHTML = "";
      lastHits = [];
      activeIndex = -1;
      return;
    }
    var hits = items.filter(function (item) {
      return matchesKind(item) && matchesLabel(item, query);
    }).slice(0, 30);
    lastHits = hits;
    activeIndex = -1;

    panel.innerHTML = hits.length ? hits.map(function (item) {
      return "<a class=\"ad-search-result\" href=\"" + esc(targetUrl(item)) + "\">"
        + "<strong>" + esc(item.label || item.qualifiedName) + "</strong>"
        + "<span>" + esc((item.kind || "") + " " + (item.displaySignature || item.qualifiedName || item.ownerName || "")) + "</span>"
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
  kindFilter.addEventListener("change", function () { render(normalizedQuery()); });
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
  document.addEventListener("click", function (event) {
    if (!panel.contains(event.target) && event.target !== input && event.target !== kindFilter) {
      panel.classList.remove("open");
      input.setAttribute("aria-expanded", "false");
    }
  });
})();
