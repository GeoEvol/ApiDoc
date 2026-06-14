<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Search - ${data.projectName}</title>
    <link rel="stylesheet" href="css/docs.css">
    <link rel="stylesheet" href="vendor/prism/prism.css">
    <link rel="stylesheet" href="vendor/github-markdown-css/github-markdown.css">
</head>
<body>
<div class="docs-layout">
    <aside class="docs-sidebar">
        <div class="docs-sidebar-inner" data-docs-sidebar data-nav-open="true">
            <div class="docs-brand-row">
                <a class="docs-brand" href="index.html"><span class="docs-brand-mark"></span>${data.projectName}</a>
                <button class="icon-button sidebar-toggle" type="button" data-sidebar-toggle aria-expanded="true" title="Toggle navigation">
                    <img src="vendor/lucide/icons/chevron-down.svg" alt="">
                </button>
            </div>
            <nav class="docs-sidebar-nav">
                <span class="docs-mobile-nav">Navigation</span>
                <a href="index.html">Overview</a>
                <a href="index-all.html">Index</a>
                <a class="active" href="search.html">Search</a>
            </nav>
            <section class="docs-package-section">
                <div class="docs-nav-label">Packages</div>
                <div class="docs-package-list">
                    <#list data.packageIndex?keys?sort as pkg>
                        <a class="docs-package-link" href="${data.packagePathMap[pkg]}">${pkg}</a>
                    </#list>
                </div>
            </section>
        </div>
    </aside>
    <main class="docs-content">
        <header class="docs-topbar">
            <div class="docs-headline">
                <div class="breadcrumb">Overview / Search</div>
                <h1>Search</h1>
            </div>
            <div class="docs-topbar-actions">
                <button class="icon-button" type="button" data-theme-toggle aria-pressed="false" title="Switch theme">
                    <img src="vendor/lucide/icons/moon.svg" alt="" data-theme-icon="dark">
                    <img src="vendor/lucide/icons/sun.svg" alt="" data-theme-icon="light" hidden>
                </button>
            </div>
        </header>
        <section class="section-card search-panel">
            <div class="search-input-wrap">
                <input id="search-input" class="search-input" type="search" placeholder="Search API" autocomplete="off">
            </div>
            <div id="search-result-section">
                <div id="search-result-container"></div>
            </div>
        </section>
    </main>
</div>
<script>var pathtoroot = "";</script>
<script src="js/theme-switcher.js"></script>
<script src="js/javadoc-navigation-adapter.js"></script>
<script src="js/javadoc-search-adapter.js"></script>
<script src="js/search.js"></script>
<script src="vendor/prism/prism.js"></script>
<script src="js/app.js"></script>
<script src="module-search-index.js"></script>
<script src="package-search-index.js"></script>
<script src="type-search-index.js"></script>
<script src="member-search-index.js"></script>
<script src="tag-search-index.js"></script>
</body>
</html>