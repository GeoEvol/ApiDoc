<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${data.projectName}</title>
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
                <a class="active" href="index.html">Overview</a>
                <a href="index-all.html">Index</a>
                <a href="search.html">Search</a>
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
                <div class="breadcrumb">Overview</div>
                <h1>${data.projectName}</h1>
            </div>
            <div class="docs-topbar-actions">
                <a class="icon-button" href="search.html" title="Search">
                    <img src="vendor/lucide/icons/search.svg" alt="">
                </a>
                <button class="icon-button" type="button" data-theme-toggle aria-pressed="false" title="Switch theme">
                    <img src="vendor/lucide/icons/moon.svg" alt="" data-theme-icon="dark">
                    <img src="vendor/lucide/icons/sun.svg" alt="" data-theme-icon="light" hidden>
                </button>
            </div>
        </header>

        <div class="page-shell">
            <section class="summary-grid">
                <div><strong>${(data.docStats.classCount)!0}</strong><span>Types</span></div>
                <div><strong>${(data.docStats.methodCount)!0}</strong><span>Methods</span></div>
                <div><strong>${(data.docStats.fieldCount)!0}</strong><span>Fields</span></div>
                <div><strong>${(data.docStats.constructorCount)!0}</strong><span>Constructors</span></div>
            </section>

            <section class="section-card">
                <h2>Packages</h2>
                <table>
                    <thead><tr><th>Package</th><th>Types</th></tr></thead>
                    <tbody>
                    <#list data.packageIndex?keys?sort as pkg>
                        <tr>
                            <td><a href="${data.packagePathMap[pkg]}"><code>${pkg}</code></a></td>
                            <td>${data.packageIndex[pkg]?size}</td>
                        </tr>
                    </#list>
                    </tbody>
                </table>
            </section>

            <section class="section-card">
                <h2>Types</h2>
                <div class="type-list">
                <#list data.apiDocs as doc>
                    <a href="${doc.path}" class="type-row">
                        <span>${doc.name}</span>
                        <small>${doc.packageName!''}</small>
                    </a>
                </#list>
                </div>
            </section>
        </div>
    </main>
</div>
<script src="js/theme-switcher.js"></script>
<script src="js/javadoc-navigation-adapter.js"></script>
<script src="vendor/prism/prism.js"></script>
<script src="js/app.js"></script>
</body>
</html>