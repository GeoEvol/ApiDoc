(function () {
    var STORAGE_KEY = "apidoc-theme";

    function applyTheme(theme) {
        document.documentElement.setAttribute("data-theme", theme);
        var toggle = document.querySelector("[data-theme-toggle]");
        if (toggle) {
            toggle.setAttribute("aria-pressed", theme === "dark" ? "true" : "false");
            toggle.setAttribute("title", theme === "dark" ? "Switch to light theme" : "Switch to dark theme");
        }
        var lightIcon = document.querySelector("[data-theme-icon='light']");
        var darkIcon = document.querySelector("[data-theme-icon='dark']");
        if (lightIcon) {
            lightIcon.hidden = theme !== "dark";
        }
        if (darkIcon) {
            darkIcon.hidden = theme === "dark";
        }
    }

    function currentTheme() {
        return localStorage.getItem(STORAGE_KEY) || "light";
    }

    function init() {
        applyTheme(currentTheme());
        var toggle = document.querySelector("[data-theme-toggle]");
        if (!toggle) {
            return;
        }
        toggle.addEventListener("click", function () {
            var next = currentTheme() === "dark" ? "light" : "dark";
            localStorage.setItem(STORAGE_KEY, next);
            applyTheme(next);
        });
    }

    window.DocsTheme = {
        init: init
    };
})();
