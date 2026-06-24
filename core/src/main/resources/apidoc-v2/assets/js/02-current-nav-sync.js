
  function findCurrentNavTarget() {
    if (!nav) return null;
    var target = nav.querySelector('.is-current[aria-current="page"]');
    if (target) return target;
    var current = normalizeNavUrl(location.href);
    var links = nav.querySelectorAll("a[href]");
    for (var i = 0; i < links.length; i++) {
      if (normalizeNavUrl(links[i].href) === current) return links[i];
    }
    return null;
  }

  function normalizeNavUrl(value) {
    var url = new URL(value, location.href);
    url.hash = "";
    return url.pathname.replace(/\/+$/g, "") + url.search;
  }

  function openCurrentNavAncestors(target) {
    for (var parent = target ? target.parentElement : null; parent && parent !== nav; parent = parent.parentElement) {
      if (parent.tagName === "DETAILS") parent.open = true;
    }
  }

  function isHiddenWithinNav(target) {
    for (var node = target; node && node !== nav; node = node.parentElement) {
      if (node.hidden) return true;
    }
    return false;
  }

  function scrollNavTargetIntoViewIfNeeded(target) {
    if (!target || !navScroll || isHiddenWithinNav(target)) return false;
    var targetRect = target.getBoundingClientRect();
    var scrollRect = navScroll.getBoundingClientRect();
    if (!targetRect.height || !scrollRect.height) return false;

    var padding = 18;
    var topDelta = targetRect.top - scrollRect.top;
    var bottomDelta = targetRect.bottom - scrollRect.bottom;
    var desired = navScroll.scrollTop;
    if (topDelta < padding) {
      desired += topDelta - padding;
    } else if (bottomDelta > -padding) {
      desired += bottomDelta + padding;
    } else {
      return true;
    }

    var max = Math.max(0, navScroll.scrollHeight - navScroll.clientHeight);
    navAutoScrolling = true;
    navScroll.scrollTop = Math.max(0, Math.min(Math.round(desired), max));
    window.setTimeout(function () { navAutoScrolling = false; }, 120);
    return true;
  }

  function syncCurrentNav(force) {
    if (!nav || !navScroll || (navUserScrolled && !force)) return;
    var target = findCurrentNavTarget();
    if (!target) return;
    openCurrentNavAncestors(target);
    scrollNavTargetIntoViewIfNeeded(target);
  }

  function scheduleCurrentNavSync(options) {
    if (!nav || !navScroll || navSyncScheduled) return;
    var force = !!(options && options.force);
    navSyncScheduled = true;
    window.requestAnimationFrame(function () {
      window.requestAnimationFrame(function () {
        navSyncScheduled = false;
        syncCurrentNav(force);
        window.setTimeout(function () { syncCurrentNav(force); }, 80);
      });
    });
  }

  if (navScroll) {
    navScroll.addEventListener("scroll", function () {
      if (!navAutoScrolling) navUserScrolled = true;
    }, { passive: true });
  }

  scheduleCurrentNavSync();
