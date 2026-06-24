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
  var navScroll = nav ? nav.querySelector(".ad-book-nav-scroll") : null;
  var navUserScrolled = false;
  var navAutoScrolling = false;
  var navSyncScheduled = false;
