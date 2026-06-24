
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
