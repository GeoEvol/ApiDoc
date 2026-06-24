
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
