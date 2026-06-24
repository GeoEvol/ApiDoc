
  var tocJumpToggle = document.querySelector(".ad-toc-jump-toggle");
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
    if (tocJumpToggle) {
      var lastScrollTop = window.scrollY;
      window.addEventListener("scroll", function () {
        if (!tocJumpToggle) return;
        var scrollY = window.scrollY;
        var scrollable = maxScrollTop();
        if (scrollable <= 0 || scrollY <= 0) {
          tocJumpToggle.hidden = true;
        } else if (scrollY < lastScrollTop) {
          tocJumpToggle.hidden = false;
        } else {
          tocJumpToggle.hidden = true;
        }
        lastScrollTop = scrollY;
      }, { passive: true });
      tocJumpToggle.addEventListener("click", function () {
        window.scrollTo({ top: 0, behavior: "smooth" });
      });
      tocJumpToggle.hidden = true;
    }
    setActiveToc();
  } else if (tocJumpToggle) {
    tocJumpToggle.hidden = true;
  }
