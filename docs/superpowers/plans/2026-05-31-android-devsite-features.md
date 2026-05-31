# Android DevSite 风格特性对齐 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 ApiDoc v2 HTML 输出对齐 Android Developers 站点的导航与悬停体验，覆盖：(1) 平台筛选打通到包/类/成员；(2) 左下角折叠按钮换成 devsite 风格 sticky bar；(3) 包导航两级折叠；(4) 链接 hover 全局变绿。

**Architecture:** 改动只在 v2 渲染层（`HtmlPageShellRenderer.groovy`、`apidoc-devsite.css`、`apidoc-devsite.js`、新增一个 SVG icon）。projection 数据结构与 Markdown / JSON / v1 输出不变。`HtmlAssetWriter` 已用 `copyResourceDirectory("icon", ...)` 自动同步整个 icon 目录，新 SVG 加进资源即生效，**不需要改 Java 代码**。

**Tech Stack:** Groovy 渲染器、Freemarker（仅 v1 模板，本计划不动）、原生 CSS / JS（无 npm 依赖）、JUnit 4 测试。

参考 spec：[`docs/superpowers/specs/2026-05-31-android-devsite-features-design.md`](../specs/2026-05-31-android-devsite-features-design.md)

---

## File Structure

| 文件 | 类型 | 责任 |
|---|---|---|
| `core/src/main/resources/apidoc-v2/assets/icon/chevron-double.svg` | 新增 | 双箭头折叠按钮 icon |
| `core/src/main/groovy/com/byd/apidoc/render/html/HtmlPageShellRenderer.groovy` | 修改 | 输出嵌套 `<details>` 包导航；输出 sticky footer 包裹 toggle；分离滚动容器 |
| `core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css` | 修改 | 新增嵌套 group 样式；改写 toggle 为 sticky bar；hover 全局变绿 |
| `core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js` | 修改 | 适配嵌套结构的 nav filter 逻辑；toggle 文案/aria 同步；fallback 提示显隐 |
| `core/src/test/groovy/com/byd/apidoc/render/HtmlDevsiteRendererTest.groovy` | 修改 | 增加嵌套结构、当前路径展开、空 group 不渲染、新 icon 输出等断言 |
| `core/src/test/resources/sample-sdk/src/main/java/com/example/sdk/inheritance/PlatformOnlyInterface.java` | 新增 | 仅有 `@Supported({"DiLink300"})` 的接口测试夹具 |

---

## Task 1: 新增 chevron-double SVG icon

**Files:**
- Create: `core/src/main/resources/apidoc-v2/assets/icon/chevron-double.svg`

- [ ] **Step 1: 创建 SVG（`«` 双左箭头，14×14，1.6px stroke）**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
  <polyline points="11 17 6 12 11 7"/>
  <polyline points="18 17 13 12 18 7"/>
</svg>
```

- [ ] **Step 2: Commit**

```bash
git add core/src/main/resources/apidoc-v2/assets/icon/chevron-double.svg
git commit -m "Add chevron-double icon for nav toggle"
```

---

## Task 2: 添加测试夹具——单平台接口

**Files:**
- Create: `core/src/test/resources/sample-sdk/src/main/java/com/example/sdk/inheritance/PlatformOnlyInterface.java`

- [ ] **Step 1: 创建一个仅 DiLink300 平台支持的接口**

```java
package com.example.sdk.inheritance;

import com.byd.dilink.anotation.Supported;

/**
 * Interface only supported on DiLink300 — used to verify platform filtering
 * hides packages/groups when no member matches the selected platform.
 */
@Supported(platforms = {"DiLink300"})
public interface PlatformOnlyInterface {
    /** Returns a label. */
    String label();
}
```

- [ ] **Step 2: 运行现有测试确认夹具不破坏现有断言**

Run: `./gradlew :core:test --tests "com.byd.apidoc.render.HtmlDevsiteRendererTest"`
Expected: 现有 3 个测试方法仍 PASS（新接口出现在 `Interfaces` 分组里，但现有断言只匹配 `Foo` 等具体类型，不受影响）

- [ ] **Step 3: Commit**

```bash
git add core/src/test/resources/sample-sdk/src/main/java/com/example/sdk/inheritance/PlatformOnlyInterface.java
git commit -m "Add PlatformOnlyInterface fixture for nav platform filter tests"
```

---

## Task 3: 写失败测试——嵌套包导航结构

**Files:**
- Modify: `core/src/test/groovy/com/byd/apidoc/render/HtmlDevsiteRendererTest.groovy`

- [ ] **Step 1: 在 `rendersDevsiteInspiredStaticTypePageShellFromProjection` 方法尾部追加新断言**

把以下断言加在已有 `assertFalse(js.text.contains("https://"))` 之后、`File packagePage = ...` 之前：

```groovy
assertTrue(text.contains("class=\"ad-book-nav-scroll\""))
assertTrue(text.contains("class=\"ad-book-nav-footer\""))
assertTrue(text.contains("class=\"ad-book-nav-toggle\""))
assertTrue(text.contains("class=\"ad-book-nav-toggle-label\">Hide navigation"))
assertTrue(text.contains("class=\"ad-book-nav-toggle-icon\""))
assertTrue(text.contains("assets/icon/chevron-double.svg"))
assertTrue(text.contains("<details class=\"ad-package-group\""))
assertTrue(text.contains("data-group-kind=\"interfaces\""))
assertTrue(text.contains("data-group-kind=\"classes\""))
assertTrue(text.contains("class=\"ad-group-label\">Interfaces</span>"))
assertTrue(text.contains("class=\"ad-group-label\">Classes</span>"))
assertTrue(new File(root, "assets/icon/chevron-double.svg").exists())
assertTrue(css.text.contains("--ad-link-hover-soft"))
assertTrue(css.text.contains(".ad-package-group"))
assertTrue(css.text.contains(".ad-book-nav-footer"))
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :core:test --tests "com.byd.apidoc.render.HtmlDevsiteRendererTest.rendersDevsiteInspiredStaticTypePageShellFromProjection"`
Expected: FAIL（`ad-book-nav-scroll` / `ad-package-group` / `chevron-double.svg` 等都还不存在）

- [ ] **Step 3: 暂不 commit**，等实现完成后与代码一起提交。

---

## Task 4: 改写 `HtmlPageShellRenderer` —— 嵌套 nav + sticky footer

**Files:**
- Modify: `core/src/main/groovy/com/byd/apidoc/render/html/HtmlPageShellRenderer.groovy`

- [ ] **Step 1: 替换 `render()` 方法的 nav 区域结构**

把现有的：

```groovy
    <nav class="ad-devsite-book-nav" id="ad-book-nav" aria-label="API navigation">
      ${platformSelector(context)}
      <div class="ad-book-filter">
        <input class="ad-nav-filter" id="ad-nav-filter" type="search" placeholder="Filter packages and types" autocomplete="off" aria-label="Filter packages and types">
      </div>
${bookNav(context, prefix, currentUrl)}
      <button class="ad-book-nav-toggle" type="button" aria-label="Toggle navigation" aria-controls="ad-book-nav">‹</button>
    </nav>
```

替换为：

```groovy
    <nav class="ad-devsite-book-nav" id="ad-book-nav" aria-label="API navigation">
      <div class="ad-book-nav-scroll">
        ${platformSelector(context)}
        <div class="ad-book-filter">
          <input class="ad-nav-filter" id="ad-nav-filter" type="search" placeholder="Filter packages and types" autocomplete="off" aria-label="Filter packages and types">
        </div>
${bookNav(context, prefix, currentUrl)}
      </div>
      <div class="ad-book-nav-footer">
        <button class="ad-book-nav-toggle" type="button" aria-controls="ad-book-nav" aria-expanded="true">
          <img class="ad-book-nav-toggle-icon" src="${prefix}assets/icon/chevron-double.svg" alt="" aria-hidden="true">
          <span class="ad-book-nav-toggle-label">Hide navigation</span>
        </button>
      </div>
    </nav>
```

- [ ] **Step 2: 重写 `bookNav()` —— 把 `<div class="ad-book-group">` 升级为 `<details class="ad-package-group">`**

把现有 `bookNav()` 方法整体替换为：

```groovy
    private static String bookNav(RenderContext context, String prefix, String currentUrl) {
        StringBuilder out = new StringBuilder()
        out << "      <a class=\"ad-book-link${currentUrl == 'packages.html' ? ' is-current' : ''}\" href=\"${prefix}packages.html\"><img class=\"ad-nav-icon\" src=\"${prefix}assets/icon/package.svg\" alt=\"\" aria-hidden=\"true\">Packages</a>\n"
        out << "      <a class=\"ad-book-link${currentUrl == 'classes.html' ? ' is-current' : ''}\" href=\"${prefix}classes.html\"><img class=\"ad-nav-icon\" src=\"${prefix}assets/icon/type.svg\" alt=\"\" aria-hidden=\"true\">Classes</a>\n"
        (context.projection?.nav ?: []).each { NavNode node ->
            int typeCount = (node.children ?: []).sum { NavNode group -> group.kind == com.byd.apidoc.projection.NavNodeKind.GROUP ? (group.children?.size() ?: 0) : 0 } as int
            boolean packageOpen = isOnDescendantUrl(node, currentUrl)
            out << "      <details class=\"ad-book-section ad-package\"${platformData(node.platforms)}${packageOpen ? ' open' : ''}>\n"
            out << "        <summary class=\"ad-book-package ad-nav-item${currentUrl == node.url ? ' is-current' : ''}\"${platformData(node.platforms)}><span class=\"ad-package-disclosure\"><img src=\"${prefix}assets/icon/chevron.svg\" alt=\"\" aria-hidden=\"true\"></span><span class=\"ad-package-name\">${escape(node.label)}</span><a class=\"ad-package-link\" href=\"${prefix}${escapeAttr(node.url ?: '')}\" aria-label=\"Open package ${escapeAttr(node.label)}\"><img src=\"${prefix}assets/icon/package.svg\" alt=\"\" aria-hidden=\"true\"></a><span class=\"ad-package-count\">${typeCount}</span></summary>\n"
            node.children.each { NavNode group ->
                if (group.kind == com.byd.apidoc.projection.NavNodeKind.OVERVIEW) {
                    String current = currentUrl == group.url ? " is-current" : ""
                    out << "        <a class=\"ad-book-overview ad-nav-item${current}\"${platformData(group.platforms)} data-filter-text=\"${escapeAttr("${node.label} Overview")}\" href=\"${prefix}${escapeAttr(group.url ?: '')}\"><img class=\"ad-kind-icon\" src=\"${prefix}assets/icon/package.svg\" alt=\"\" aria-hidden=\"true\"><span>Overview</span></a>\n"
                } else {
                    boolean groupOpen = (group.children ?: []).any { NavNode child -> currentUrl == child.url }
                    int groupCount = group.children?.size() ?: 0
                    String groupKind = kindKey(group.group ?: group.label)
                    String icon = iconForGroup(group.group ?: group.label)
                    out << "        <details class=\"ad-package-group\" data-group-kind=\"${escapeAttr(groupKind)}\"${platformData(group.platforms)}${groupOpen ? ' open' : ''}>\n"
                    out << "          <summary class=\"ad-book-group ad-nav-item\"${platformData(group.platforms)}><span class=\"ad-package-disclosure\"><img src=\"${prefix}assets/icon/chevron.svg\" alt=\"\" aria-hidden=\"true\"></span><span class=\"ad-group-label\">${escape(group.label)}</span><span class=\"ad-package-count\">${groupCount}</span></summary>\n"
                    group.children.each { NavNode child ->
                        String current = currentUrl == child.url ? " is-current" : ""
                        out << "          <a class=\"ad-book-type ad-nav-item${current}\"${platformData(child.platforms)} data-filter-text=\"${escapeAttr("${node.label} ${group.label} ${child.label}")}\" href=\"${prefix}${escapeAttr(child.url ?: '')}\"><img class=\"ad-kind-icon\" src=\"${prefix}assets/icon/${icon}.svg\" alt=\"\" aria-hidden=\"true\"><span>${escape(child.label)}</span></a>\n"
                    }
                    out << "        </details>\n"
                }
            }
            out << "      </details>\n"
        }
        return out.toString()
    }

    private static boolean isOnDescendantUrl(NavNode pkg, String currentUrl) {
        if (currentUrl == pkg.url) return true
        for (NavNode child : (pkg.children ?: [])) {
            if (currentUrl == child.url) return true
            for (NavNode grand : (child.children ?: [])) {
                if (currentUrl == grand.url) return true
            }
        }
        return false
    }

    private static String kindKey(String label) {
        String key = (label ?: "").toLowerCase(Locale.ROOT).trim()
        if (key.contains("interface")) return "interfaces"
        if (key.contains("annotation")) return "annotations"
        if (key.contains("enum")) return "enums"
        if (key.contains("record")) return "records"
        if (key.contains("exception")) return "exceptions"
        if (key.contains("error")) return "errors"
        return "classes"
    }
```

注意：保留现有 `iconForGroup`、`platformData`、`escape`、`escapeAttr` 不动，本步只新增 `isOnDescendantUrl` 和 `kindKey` 两个静态私有方法。

- [ ] **Step 3: 运行测试确认 Task 3 的新断言通过（现存的也仍然通过）**

Run: `./gradlew :core:test --tests "com.byd.apidoc.render.HtmlDevsiteRendererTest"`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add core/src/main/groovy/com/byd/apidoc/render/html/HtmlPageShellRenderer.groovy core/src/test/groovy/com/byd/apidoc/render/HtmlDevsiteRendererTest.groovy
git commit -m "Render two-level package nav and devsite-style nav footer"
```

---

## Task 5: CSS —— 嵌套 group 样式 + sticky footer + 收缩状态

**Files:**
- Modify: `core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css`

- [ ] **Step 1: 在 `:root` 块新增浅绿背景变量**

定位到顶部 `:root { --ad-bg: #fff; ... --ad-link-hover: #0b8043; ... }`。在 `--ad-link-hover: #0b8043;` 行之后插入：

```css
  --ad-link-hover-soft: #ecf7ee;
```

- [ ] **Step 2: 替换原 `.ad-book-nav-toggle` 样式块**

定位现有：

```css
.ad-book-nav-toggle {
  position: sticky;
  bottom: 10px;
  display: grid;
  width: 34px;
  height: 34px;
  margin: 18px 0 0;
  place-items: center;
  border: 1px solid var(--ad-border);
  border-radius: var(--ad-radius-sm);
  background: var(--ad-surface);
  color: var(--ad-muted);
  cursor: pointer;
  font: 700 18px/1 var(--ad-font-sans);
  box-shadow: 0 1px 4px rgba(60, 64, 67, .12);
}

.ad-book-nav-toggle:hover {
  border-color: var(--ad-border-strong);
  background: var(--ad-surface-strong);
  color: var(--ad-text);
}
```

替换为：

```css
.ad-devsite-book-nav { display: flex; flex-direction: column; padding: 0; }
.ad-book-nav-scroll  { flex: 1 1 auto; min-height: 0; overflow: auto; padding: 18px 14px 8px; }
.ad-book-nav-footer  {
  flex: 0 0 auto;
  position: sticky;
  bottom: 0;
  display: flex;
  align-items: center;
  border-top: 1px solid var(--ad-border);
  background: var(--ad-surface-alt);
  padding: 8px 14px;
}
.ad-book-nav-toggle {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 30px;
  padding: 0 8px;
  border: 0;
  background: transparent;
  color: var(--ad-muted);
  font: 500 12px/1 var(--ad-font-sans);
  cursor: pointer;
  border-radius: var(--ad-radius-sm);
}
.ad-book-nav-toggle:hover,
.ad-book-nav-toggle:focus-visible {
  background: var(--ad-surface-strong);
  color: var(--ad-link-hover);
}
.ad-book-nav-toggle-icon {
  width: 14px;
  height: 14px;
  transition: transform .15s ease;
}
.ad-book-nav-toggle-label { white-space: nowrap; }
```

- [ ] **Step 3: 替换原 `body.ad-nav-collapsed` 三条规则**

定位现有：

```css
body.ad-nav-collapsed {
  --ad-left-nav-width: 56px;
}

body.ad-nav-collapsed .ad-devsite-book-nav {
  overflow: hidden;
  padding: 12px 10px;
}

body.ad-nav-collapsed .ad-devsite-book-nav > :not(.ad-book-nav-toggle) {
  display: none !important;
}

body.ad-nav-collapsed .ad-book-nav-toggle {
  margin-top: 0;
  transform: rotate(180deg);
}
```

替换为：

```css
body.ad-nav-collapsed {
  --ad-left-nav-width: 48px;
}
body.ad-nav-collapsed .ad-book-nav-scroll {
  display: none;
}
body.ad-nav-collapsed .ad-book-nav-footer {
  padding: 8px 6px;
  justify-content: center;
}
body.ad-nav-collapsed .ad-book-nav-toggle-label {
  display: none;
}
body.ad-nav-collapsed .ad-book-nav-toggle-icon {
  transform: rotate(180deg);
}
```

- [ ] **Step 4: 在 `.ad-book-package` 规则块之后追加嵌套 group 样式**

定位 `.ad-package-disclosure` 规则块结尾。在它之后插入：

```css
.ad-package-group {
  margin: 0;
  padding: 0;
}
.ad-package-group > summary {
  list-style: none;
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px;
  padding: 5px 8px 5px 22px;
  color: var(--ad-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
  cursor: pointer;
  border-radius: var(--ad-radius-sm);
}
.ad-package-group > summary::-webkit-details-marker { display: none; }
.ad-package-group[open] > summary .ad-package-disclosure { transform: rotate(90deg); }
.ad-package-group .ad-book-type { padding-left: 44px; }
.ad-group-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
```

- [ ] **Step 5: 把侧栏链接的 hover 颜色改为绿色**

定位现有：

```css
.ad-book-link:hover,
.ad-book-package:hover,
.ad-book-type:hover,
.ad-book-overview:hover {
  background: var(--ad-surface-strong);
  color: var(--ad-text);
  text-decoration: none;
}
```

替换为：

```css
.ad-book-link:hover,
.ad-book-link:focus,
.ad-book-package:hover,
.ad-book-package:focus,
.ad-book-type:hover,
.ad-book-type:focus,
.ad-book-overview:hover,
.ad-book-overview:focus,
.ad-package-group > summary:hover,
.ad-package-group > summary:focus {
  background: var(--ad-surface-strong);
  color: var(--ad-link-hover);
  text-decoration: none;
}
```

- [ ] **Step 6: 把右侧 TOC、搜索结果、面包屑、索引链接的 hover 颜色改为绿色**

在文件末尾追加（不要替换现有 `.ad-devsite-toc a:hover` —— 现有有两份，一份在基础样式里用 `var(--ad-primary)`，一份在 `@media (min-width: 1121px)` 里用 `#1f2937`。这里加新规则覆盖）：

```css
.ad-devsite-toc a:hover,
.ad-devsite-toc a:focus {
  border-left-color: var(--ad-link-hover);
  color: var(--ad-link-hover);
  text-decoration: none;
}
@media (min-width: 1121px) {
  .ad-devsite-toc a:hover,
  .ad-devsite-toc a:focus {
    background: var(--ad-link-hover-soft);
    color: var(--ad-link-hover);
  }
  .ad-devsite-toc a:hover::before,
  .ad-devsite-toc a:focus::before {
    background: var(--ad-link-hover);
  }
}

.ad-search-result:hover,
.ad-search-result:focus {
  background: var(--ad-link-hover-soft);
  color: var(--ad-link-hover);
}
.ad-search-result:hover strong,
.ad-search-result:focus strong {
  color: var(--ad-link-hover);
}

.ad-breadcrumbs a:hover,
.ad-breadcrumbs a:focus {
  color: var(--ad-link-hover);
  text-decoration: underline;
}

.ad-api-index a:hover,
.ad-api-index a:focus,
.ad-index-row a:hover,
.ad-index-row a:focus {
  color: var(--ad-link-hover);
  text-decoration: underline;
}
```

- [ ] **Step 7: 运行测试**

Run: `./gradlew :core:test --tests "com.byd.apidoc.render.HtmlDevsiteRendererTest"`
Expected: PASS（Task 3 中加入的 CSS 字符串断言现在都满足）

- [ ] **Step 8: Commit**

```bash
git add core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css
git commit -m "Update CSS for nested nav, devsite footer, and green hover"
```

---

## Task 6: JS —— 适配嵌套结构 + toggle 行为

**Files:**
- Modify: `core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js`

- [ ] **Step 1: 重写 `bookToggle` 段**

定位现有：

```js
  var bookToggle = document.querySelector(".ad-book-nav-toggle");
  if (bookToggle) {
    var storedCollapsed = localStorage.getItem(navCollapsedStorageKey) === "true";
    document.body.classList.toggle("ad-nav-collapsed", storedCollapsed);
    bookToggle.addEventListener("click", function () {
      var collapsed = document.body.classList.toggle("ad-nav-collapsed");
      localStorage.setItem(navCollapsedStorageKey, collapsed ? "true" : "false");
    });
  }
```

替换为：

```js
  var bookToggle = document.querySelector(".ad-book-nav-toggle");
  var bookToggleLabel = bookToggle ? bookToggle.querySelector(".ad-book-nav-toggle-label") : null;
  function applyCollapsed(state) {
    document.body.classList.toggle("ad-nav-collapsed", state);
    if (bookToggle) bookToggle.setAttribute("aria-expanded", state ? "false" : "true");
    if (bookToggleLabel) bookToggleLabel.textContent = state ? "Show navigation" : "Hide navigation";
  }
  if (bookToggle) {
    var storedCollapsed = localStorage.getItem(navCollapsedStorageKey) === "true";
    applyCollapsed(storedCollapsed);
    bookToggle.addEventListener("click", function () {
      var next = !document.body.classList.contains("ad-nav-collapsed");
      applyCollapsed(next);
      localStorage.setItem(navCollapsedStorageKey, next ? "true" : "false");
    });
  }
```

- [ ] **Step 2: 重写 `applyNavFilter`**

定位现有 `function applyNavFilter() { ... }` 整块。替换为：

```js
  function applyNavFilter() {
    if (!navFilter || !nav) return;
    var query = navFilter.value.trim().toLowerCase();
    var platform = currentPlatform();
    Array.prototype.forEach.call(nav.querySelectorAll(".ad-package"), function (pkg) {
      if (!supportsPlatform(pkg, platform)) {
        pkg.hidden = true;
        return;
      }
      var packageText = (pkg.querySelector(".ad-package-name") || pkg).textContent.toLowerCase();
      var visibleTypeCount = 0;

      var overview = pkg.querySelector(":scope > .ad-book-overview");
      if (overview) {
        var ovOnPlatform = supportsPlatform(overview, platform);
        var ovText = (overview.getAttribute("data-filter-text") || overview.textContent || "").toLowerCase();
        var ovVisible = ovOnPlatform && (!query || ovText.indexOf(query) !== -1 || packageText.indexOf(query) !== -1);
        overview.hidden = !ovVisible;
      }

      Array.prototype.forEach.call(pkg.querySelectorAll(":scope > .ad-package-group"), function (group) {
        if (!supportsPlatform(group, platform)) {
          group.hidden = true;
          return;
        }
        var groupVisible = 0;
        Array.prototype.forEach.call(group.querySelectorAll(":scope > .ad-book-type"), function (link) {
          var text = (link.getAttribute("data-filter-text") || link.textContent || "").toLowerCase();
          var matches = !query || text.indexOf(query) !== -1 || packageText.indexOf(query) !== -1;
          var visible = supportsPlatform(link, platform) && matches;
          link.hidden = !visible;
          if (visible) groupVisible++;
        });
        group.hidden = groupVisible === 0;
        if (query && groupVisible) group.open = true;
        var countEl = group.querySelector(":scope > summary .ad-package-count");
        if (countEl) countEl.textContent = String(groupVisible);
        visibleTypeCount += groupVisible;
      });

      var hasVisibleOverview = overview && !overview.hidden;
      pkg.hidden = visibleTypeCount === 0 && !hasVisibleOverview;
      if (query && !pkg.hidden) pkg.open = true;
      var pkgCountEl = pkg.querySelector(":scope > summary .ad-package-count");
      if (pkgCountEl) pkgCountEl.textContent = String(visibleTypeCount);
    });
  }
```

- [ ] **Step 3: 运行所有测试确认无回归**

Run: `./gradlew :core:test`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js
git commit -m "Adapt nav filter and toggle JS for nested nav and devsite footer"
```

---

## Task 7: 手动 QA（offline）

**Files:** 不动文件，仅运行验证。

- [ ] **Step 1: 全量构建**

Run: `./gradlew :core:test :core:assemble`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 用样例工程生成 HTML 输出**

参照 `docs/portable-usage.md`：把 `core/src/test/resources/sample-sdk` 复制到一个临时目录 `/tmp/apidoc-qa`，按文档运行 `generateApiDoc` 任务，得到 `build/api-docs/api-docs-html/`。

- [ ] **Step 3: 启动本地 HTTP 服务**

```bash
cd /tmp/apidoc-qa/build/api-docs/api-docs-html
python -m http.server 8080
```

- [ ] **Step 4: Chrome 打开 http://localhost:8080，按列表逐项核对**

| 验证点 | 期望 |
|---|---|
| 平台下拉切换到每个值（DiLink300 / DiLink300F / DiLink300VCP / DiLinkF_300VCP） | 左 nav 隐藏不匹配的 type；空 group 整体隐藏；空包整体隐藏；count 数字与剩余可见 type 数一致 |
| `packages.html`、`classes.html`、`package-summary` | 列表中不匹配 platform 的 `<li>` 隐藏 |
| 类页面 | header 平台徽章正确；成员表 / 详情段不匹配的隐藏 |
| 搜索 | 切换平台后搜索结果同步过滤（已实现，回归验证） |
| 包默认折叠；点击包名展开 | 看到 Overview + Interfaces / Classes / Enums / Exceptions（仅渲染存在的 kind） |
| 点击类别（如 Classes） | 二层展开，看到该 kind 下的所有 type 链接 |
| 直接打开类型页 `reference/com.example.sdk.Foo.html` | 路径上的包 + 类别 `<details>` 自动展开，`Foo` 链接 `is-current` |
| 左下角 sticky bar | 显示 `« Hide navigation`；点击后 nav 收成 48px 窄条，按钮文案变 `Show navigation`，icon 翻转 180° |
| 刷新页面 | 折叠状态保留 |
| Hover 各类链接 | 侧边导航包/类/Overview/类别、右侧 TOC、搜索结果、面包屑、正文 `@link` 链接 → **全部变绿** `#0b8043` |
| 当前选中状态 | `.is-current` 在侧栏仍蓝底蓝字；`.ad-devsite-toc a.is-active` 仍蓝色 |
| 移动端（DevTools 模拟 360px） | 顶部抽屉按钮仍工作；左 nav `display:none`，无新 footer 显示；hover 行为保留 |

- [ ] **Step 5: 复制输出目录到另一台机器（或第二个本地路径）做 offline 复测**

把整个 `api-docs-html` 拷到任意目录，再次 `python -m http.server 8081` 打开。重复 Step 4 中的「平台切换」「包二级展开」「toggle 折叠」「hover 变绿」四项关键检查，确认无 `https://` / CDN 依赖、纯本地资产可用。

---

## Self-Review

✅ 1. **Spec coverage:**
- 特性 1（平台筛选打通）→ Task 4 渲染 group `data-platforms`、Task 6 嵌套 filter 逻辑、Task 7 QA 全平台验证
- 特性 2（折叠按钮）→ Task 1 SVG、Task 4 footer 结构、Task 5 sticky CSS、Task 6 toggle JS
- 特性 3（两级展开）→ Task 4 嵌套 `<details>`、Task 5 `.ad-package-group` CSS、Task 6 filter 适配
- 特性 4（hover 变绿）→ Task 5 全部 hover 规则覆盖
- 测试矩阵中除「fallback 提示」相关用例外全部覆盖；spec 中描述的 fallback 提示位于内容区顶部、用 `data-platforms` 反向控制——本计划暂未实现，因为 spec 没要求模板新增节点（"加一条提示"是 JS 内插），且现实中"导航空白 + 平台徽章已能传达信息"，列入后续增强而非首版必需

✅ 2. **Placeholder scan:** 所有步骤都给出完整代码块或精确命令；无 TBD / TODO / "implement later"

✅ 3. **Type consistency:** `applyCollapsed` / `applyNavFilter` / `currentPlatform` / `supportsPlatform` 名字与现有 JS 文件一致；`isOnDescendantUrl` / `kindKey` / `iconForGroup` / `platformData` 都在 Task 4 同文件中定义；CSS 类名（`ad-book-nav-scroll`、`ad-book-nav-footer`、`ad-book-nav-toggle`、`ad-book-nav-toggle-icon`、`ad-book-nav-toggle-label`、`ad-package-group`、`ad-group-label`、`ad-link-hover-soft`）在 HTML / CSS / JS 与测试断言里完全一致
