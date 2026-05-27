(function () {
    function init() {
        var sidebar = document.querySelector("[data-docs-sidebar]");
        var toggle = document.querySelector("[data-sidebar-toggle]");
        if (!sidebar || !toggle) {
            return;
        }
        if (!sidebar.getAttribute("data-nav-open")) {
            sidebar.setAttribute("data-nav-open", window.innerWidth > 960 ? "true" : "false");
        }
        toggle.addEventListener("click", function () {
            var next = sidebar.getAttribute("data-nav-open") !== "true";
            sidebar.setAttribute("data-nav-open", next ? "true" : "false");
            toggle.setAttribute("aria-expanded", next ? "true" : "false");
        });
    }

    window.DocsNavigation = {
        init: init
    };
})();
