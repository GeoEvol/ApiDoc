(function () {
    function boot() {
        if (window.DocsTheme && typeof window.DocsTheme.init === "function") {
            window.DocsTheme.init();
        }
        if (window.DocsNavigation && typeof window.DocsNavigation.init === "function") {
            window.DocsNavigation.init();
        }
        if (window.Prism && typeof window.Prism.highlightAll === "function") {
            window.Prism.highlightAll();
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", boot, { once: true });
    } else {
        boot();
    }
})();
