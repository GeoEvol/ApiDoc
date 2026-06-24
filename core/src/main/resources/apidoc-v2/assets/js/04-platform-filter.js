
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
