<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${doc.name} - ${data.projectName}</title>
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
                        <a class="docs-package-link<#if packageName == doc.packageName> active</#if>" href="${utils.relativePath(docPath, data.packagePathMap[packageName])}">${packageName}</a>
                    </#list>
                </div>
            </section>
        </div>
    </aside>
    <main class="docs-content">
        <header class="docs-topbar">
            <div class="docs-headline">
                <div class="breadcrumb">
                    <a href="${utils.relativePath(docPath, data.overviewPath)}">Overview</a>
                    <#if doc.packageName?has_content>
                        / <a href="${utils.relativePath(docPath, data.packagePathMap[doc.packageName])}">${doc.packageName}</a>
                    </#if>
                </div>
                <h1>${doc.name}</h1>
                <p class="muted">${doc.qualifiedName}</p>
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
            <div class="doc-comment markdown-body">
                <#if doc.description?has_content><p>${data.typeLinker.linkifyDescription(doc.description, docPath)}</p></#if>
            </div>
            <div class="api-meta">
                <#if doc.superClass?has_content><span class="pill">Extends: ${data.typeLinker.linkifyGeneric(doc.superClass, docPath)}</span></#if>
                <#if doc.interfaces?has_content><span class="pill">Implements: <#list doc.interfaces as i>${data.typeLinker.linkifyGeneric(i, docPath)}<#if i_has_next>, </#if></#list></span></#if>
            </div>
        </section>

        <#if doc.fields?has_content>
        <section class="section-card">
            <h2>Field Summary</h2>
            <table>
                <thead><tr><th>Modifier</th><th>Name</th><th>Type</th><th>Description</th></tr></thead>
                <tbody>
                <#list doc.fields as f>
                    <tr id="${f.anchorId}">
                        <td><code>${f.modifier!''}</code></td>
                        <td><a href="#${f.anchorId}">${f.name}</a></td>
                        <td><code>${data.typeLinker.linkifyGeneric(f.type!'', docPath)}</code></td>
                        <td>${data.typeLinker.linkifyDescriptionForTableCell(f.description!'', docPath)}</td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </section>
        </#if>

        <#if doc.enumConstants?has_content>
        <section class="section-card">
            <h2>Enum Constant Summary</h2>
            <table>
                <thead><tr><th>Name</th><th>Description</th></tr></thead>
                <tbody>
                <#list doc.enumConstants as c>
                    <tr id="${c.name}">
                        <td><a href="#${c.name}">${c.name}</a></td>
                        <td>${data.typeLinker.linkifyDescriptionForTableCell(c.description!'', docPath)}</td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </section>
        </#if>

        <#if doc.constructors?has_content>
        <section class="section-card">
            <h2>Constructor Summary</h2>
            <table>
                <thead><tr><th>Constructor</th><th>Description</th></tr></thead>
                <tbody>
                <#list doc.constructors as m>
                    <tr>
                        <td><a href="#${m.anchorId}"><code>${m.signature}</code></a></td>
                        <td>${data.typeLinker.linkifyDescriptionForTableCell(m.description!'', docPath)}</td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </section>
        </#if>

        <#if doc.list?has_content>
        <section class="section-card">
            <h2>Method Summary</h2>
            <table>
                <thead><tr><th>Modifier and Type</th><th>Method</th><th>Description</th></tr></thead>
                <tbody>
                <#list doc.list as m>
                    <tr>
                        <td><code>${m.modifiers!''} ${data.typeLinker.linkifyGeneric(m.returnType!'', docPath)}</code></td>
                        <td><a href="#${m.anchorId}"><code>${m.signature}</code></a></td>
                        <td>${data.typeLinker.linkifyDescriptionForTableCell(m.description!'', docPath)}</td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </section>

        <section class="section-card">
            <h2>Method Details</h2>
            <#list doc.list as m>
            <article class="member-detail" id="${m.anchorId}">
                <h3>${m.name}</h3>
                <pre class="member-signature line-numbers"><code class="language-java">${m.modifiers!''} ${m.returnType!''} ${m.signature}</code></pre>
                <#if m.description?has_content><div class="doc-comment markdown-body"><p>${data.typeLinker.linkifyDescription(m.description, docPath)}</p></div></#if>
                <#if m.parameters?has_content>
                <h4>Parameters</h4>
                <table>
                    <thead><tr><th>Name</th><th>Type</th><th>Description</th></tr></thead>
                    <tbody>
                    <#list m.parameters as p>
                        <tr><td><code>${p.name}</code></td><td><code>${data.typeLinker.linkifyGeneric(p.type!'', docPath)}</code></td><td>${data.typeLinker.linkifyDescriptionForTableCell(p.description!'', docPath)}</td></tr>
                    </#list>
                    </tbody>
                </table>
                </#if>
                <#if m.returnComment?has_content><p><strong>Returns:</strong> ${data.typeLinker.linkifyDescription(m.returnComment, docPath)}</p></#if>
                <#if m.exceptions?has_content><p><strong>Throws:</strong> <#list m.exceptions as ex>${data.typeLinker.linkifyGeneric(ex, docPath)}<#if ex_has_next>, </#if></#list></p></#if>
            </article>
            </#list>
        </section>
        </#if>
    </main>
</div>
<script src="${utils.relativePath(docPath, 'js/theme-switcher.js')}"></script>
<script src="${utils.relativePath(docPath, 'js/javadoc-navigation-adapter.js')}"></script>
<script src="${utils.relativePath(docPath, 'vendor/prism/prism.js')}"></script>
<script src="${utils.relativePath(docPath, 'js/app.js')}"></script>
</body>
</html>
