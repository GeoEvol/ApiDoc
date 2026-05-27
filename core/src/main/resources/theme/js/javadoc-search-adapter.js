(function () {
    var groups = [
        ["Types", function () { return window.typeSearchIndex || []; }, "types"],
        ["Members", function () { return window.memberSearchIndex || []; }, "members"],
        ["Packages", function () { return window.packageSearchIndex || []; }, "packages"],
        ["Tags", function () { return window.tagSearchIndex || []; }, "searchTags"],
        ["Modules", function () { return window.moduleSearchIndex || []; }, "modules"]
    ];

    function indexFilesLoaded() {
        return Array.isArray(window.moduleSearchIndex)
            && Array.isArray(window.packageSearchIndex)
            && Array.isArray(window.typeSearchIndex)
            && Array.isArray(window.memberSearchIndex)
            && Array.isArray(window.tagSearchIndex);
    }

    function bindInput() {
        var input = document.getElementById("search-input");
        if (!input || input.dataset.docsBound === "true") {
            return;
        }
        input.dataset.docsBound = "true";
        input.addEventListener("input", runSearch);
    }

    function runSearch() {
        var input = document.getElementById("search-input");
        var container = document.getElementById("search-result-container");
        if (!input || !container || !indexFilesLoaded()) {
            return;
        }
        var query = input.value.trim().toLowerCase();
        container.innerHTML = "";
        if (!query) {
            container.innerHTML = '<p class="search-empty">Type, package, member, and tag results appear here.</p>';
            return;
        }

        groups.forEach(function (group) {
            var matches = group[1]().filter(function (item) {
                var text = [item.l, item.p, item.c, item.d].filter(Boolean).join(" ").toLowerCase();
                return text.indexOf(query) >= 0;
            }).slice(0, 50);
            if (!matches.length) {
                return;
            }
            var section = document.createElement("section");
            section.className = "search-category";
            var heading = document.createElement("h2");
            heading.textContent = group[0];
            section.appendChild(heading);
            matches.forEach(function (item) {
                section.appendChild(createResult(item, group[2]));
            });
            container.appendChild(section);
        });
    }

    function createResult(item, category) {
        var link = document.createElement("a");
        link.className = "search-result";
        link.href = getURL(item, category);
        var detail = [item.p, item.c, item.d].filter(Boolean).join(" / ");
        link.innerHTML =
            "<strong>" + escapeHtml(item.l || "") + "</strong>" +
            "<small>" + escapeHtml(detail) + "</small>";
        return link;
    }

    function getURL(item, category) {
        if (category === "packages") {
            return item.u || ((item.l || "").replace(/\./g, "/") + "/package-summary.html");
        }
        if (category === "types") {
            return item.u || (((item.p || "").replace(/\./g, "/") + "/") + item.l + ".html");
        }
        if (category === "members") {
            return ((item.p || "").replace(/\./g, "/") + "/" + item.c + ".html#" + (item.u || item.l));
        }
        return item.u || "#";
    }

    function escapeHtml(text) {
        return String(text || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function updateSearchResults() {
        bindInput();
        runSearch();
    }

    window.JavadocSearch = {
        updateSearchResults: updateSearchResults,
        runSearch: runSearch
    };
    window.updateSearchResults = updateSearchResults;

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", updateSearchResults, { once: true });
    } else {
        updateSearchResults();
    }
})();
