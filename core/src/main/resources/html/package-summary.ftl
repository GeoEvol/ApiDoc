<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${pkg} - ${data.projectName}</title>
    <link rel="stylesheet" href="${utils.relativePath(docPath, 'css/docs.css')}">
    <link rel="stylesheet" href="${utils.relativePath(docPath, 'vendor/prism/prism.css')}">
    <link rel="stylesheet" href="${utils.relativePath(docPath, 'vendor/github-markdown-css/github-markdown.css')}">
</head>
<body>
<div class="docs-layout">
    <aside class="docs-sidebar">
        <div class="docs-sidebar-inner" data-docs-sidebar data-nav-open="true">
            <div class="docs-brand-row">
                <a class="docs-brand" href="${utils.relativePath(docPath, data.overviewPath)}"><span class="docs-brand-mark"></span>${data.projectName}</a>
                <button class="icon-button sidebar-toggle" type="button" data-sidebar-toggle aria-expanded="true" title="Toggle navigation">
                    <img src="${utils.relativePath(docPath, 'vendor/lucide/icons/chevron-down.svg')}" alt="">
                </button>
            </div>
            <nav class="docs-sidebar-nav">
                <span class="docs-mobile-nav">Navigation</span>
                <a href="${utils.relativePath(docPath, data.overviewPath)}">Overview</a>
                <a href="${utils.relativePath(docPath, data.indexAllPath)}">Index</a>
                <a href="${utils.relativePath(docPath, data.searchPath)}">Search</a>
            </nav>
            <section class="docs-package-section">
                <div class="docs-nav-label">Packages</div>
                <div class="docs-package-list">
                    <#list data.packageIndex?keys?sort as packageName>
                        <a class="docs-package-link<#if packageName == pkg> active</#if>" href="${utils.relativePath(docPath, data.packagePathMap[packageName])}">${packageName}</a>
                    </#list>
                </div>
            </section>
        </div>
    </aside>
    <main class="docs-content">
        <header class="docs-topbar">
            <div class="docs-headline">
                <div class="breadcrumb"><a href="${utils.relativePath(docPath, data.overviewPath)}">Overview</a> / ${pkg}</div>
                <h1>Package ${pkg}</h1>
            </div>
            <div class="docs-topbar-actions">
                <a class="icon-button" href="${utils.relativePath(docPath, data.searchPath)}" title="Search">
                    <img src="${utils.relativePath(docPath, 'vendor/lucide/icons/search.svg')}" alt="">
                </a>
                <button class="icon-button" type="button" data-theme-toggle aria-pressed="false" title="Switch theme">
                    <img src="${utils.relativePath(docPath, 'vendor/lucide/icons/moon.svg')}" alt="" data-theme-icon="dark">
                    <img src="${utils.relativePath(docPath, 'vendor/lucide/icons/sun.svg')}" alt="" data-theme-icon="light" hidden>
                </button>
            </div>
        </header>
        <section class="section-card">
            <h2>Types</h2>
            <table>
                <thead><tr><th>Name</th><th>Description</th></tr></thead>
                <tbody>
                <#list docs as item>
                    <tr>
                        <td><a href="${utils.relativePath(docPath, item.path)}">${item.name}</a></td>
                        <td>${data.typeLinker.linkifyDescriptionForTableCell(item.description!'', docPath)}</td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </section>
    </main>
</div>
<script src="${utils.relativePath(docPath, 'js/theme-switcher.js')}"></script>
<script src="${utils.relativePath(docPath, 'js/javadoc-navigation-adapter.js')}"></script>
<script src="${utils.relativePath(docPath, 'vendor/prism/prism.js')}"></script>
<script src="${utils.relativePath(docPath, 'js/app.js')}"></script>
</body>
</html>
