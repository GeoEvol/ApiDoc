# Android DevSite 风格特性对齐设计

- 范围：v2 HTML 渲染管线（`apidoc-v2`）的网页端体验
- 目标：与 Android Developers (developer.android.google.cn/reference) 的导航、筛选、悬停体验对齐
- 不在范围内：Markdown / JSON / v1 HTML 输出格式不变；projection 数据结构不变

## 背景

ApiDoc v2 已经搭建了 devsite 风格的整体外观（CSS 变量、左 nav / 中 content / 右 TOC 三栏、API 状态 chip、嵌套 details 的包导航）。最近一次提交 `a727eb6` 引入了平台筛选骨架（`@Supported` 注解、`PlatformFilterModel`、`<select id="ad-platform-select">`、`data-platforms` 属性、JS 联动）。在此基础上还需要四项补齐，使体验真正对齐 Android Developers 站点：

1. 平台筛选的过滤粒度需要打通到包/类/成员三层
2. 左下角的折叠按钮需要换成 devsite 风格的 sticky 底部条
3. 包导航需要从一层展开升级到两层折叠（包 → 类别 → 类型）
4. 链接悬停颜色需要全局统一为绿色

## 总体架构

| 模块 | 改动类型 | 说明 |
|---|---|---|
| `core/src/main/groovy/com/byd/apidoc/render/html/HtmlPageShellRenderer.groovy` | 重写 `bookNav()` 与 `render()` | 嵌套 `<details>` 包导航；底部 sticky footer 容纳折叠按钮 |
| `core/src/main/groovy/com/byd/apidoc/render/html/HtmlSiteRenderer.groovy` | 不动数据，仅校对 `data-platforms` 是否覆盖完整 | 包页面索引 li、类页面 header、成员行已携带，无新增 |
| `core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css` | 新增 + 修正 hover | 嵌套 group 缩进/折叠样式、底部 sticky 条、hover 全局变绿 |
| `core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js` | 扩展 toggle、过滤、count | 嵌套折叠点击；按平台重算 count；平台筛 nav 时同步隐藏空 group |
| `core/src/main/resources/apidoc-v2/assets/icon/chevron-double.svg` | 新增 | devsite 风格双箭头图标 |
| `core/src/main/groovy/com/byd/apidoc/render/html/HtmlAssetWriter.groovy` | 注册新 icon | 复制到输出目录 |

projection 层完全不变。`NavigationProjection` 已经按 `package → group(kind) → type` 三层提供数据。

## 特性 1：平台筛选（全层级过滤）

### 现状

- `PlatformFilterModel` 由 `ProjectionBuilder` 收集 `@Supported(platforms = {...})` 中出现过的所有平台名，去重排序
- `HtmlPageShellRenderer.platformSelector()` 在左 nav 顶部输出 `<select id="ad-platform-select">`，第一项 `All platforms`
- `data-platforms` 属性已撒在：`PageModel.platforms` 输出处（packages.html、classes.html 列表）；`PackagePageModel` 输出（类型行、子组）；`TypePageModel.platforms`（类页面 header 与成员行）；`NavNode` 树（包/group/type）
- `apidoc-devsite.js`：`applyPlatformFilter()` 设置 `.ad-platform-hidden` 到所有 `[data-platforms]`；监听 select change；持久化到 `localStorage["apidoc.platform"]`；触发 `apidoc-platform-change` 事件
- `apidoc-search.js` 已经按 `currentPlatform()` 过滤搜索索引

### 需要补齐

1. 嵌套 group 的 `data-platforms`：当 `<div class="ad-book-group">` 升级为 `<details class="ad-package-group">` 后，新的 `<details>`、`<summary>` 都需要带 `data-platforms="<group 聚合平台>"`。聚合规则：group 平台 = 该 group 下所有 type 的 platforms 并集。已经在 `NavigationProjection` 的 group node 里有 `platforms`，渲染端写到 `<details>` 与 `<summary>` 上。

2. 包级空 group 的隐藏逻辑：`applyNavFilter()` 当前在选中平台后逐个 `<details class="ad-package">` 上判断「至少有一个可见 type」。新结构下需要扩展：
   - 先在每个 `.ad-package-group` 上判断「至少有一个可见 type」→ 否则把整个 group `<details>` 设 `hidden=true`
   - 再在每个 `.ad-package` 上判断「至少有一个可见 group」→ 否则把整个包 `<details>` 设 `hidden=true`
   - count 文本同步：`.ad-package-count` 与 group 内的 `.ad-package-count` 由 JS 用可见子项数重写

3. classes.html / packages.html / package-summary 三类清单页里所有 `<li class="ad-index-row">` 已经带 `data-platforms`（确认，无需新增）。

4. 类型页内成员表格的 `<tr data-platforms="...">` 与 `.ad-member-detail[data-platforms]` 已存在。**保持现状**，由统一的 `applyPlatformFilter()` 隐藏不匹配行/段。

### 边界

- 选中平台后该平台没有任何包：左 nav 空白；中间内容区如果当前页就是被过滤掉的类型，**不做导航跳转**，仅在内容顶部加一条提示带 `data-platforms` 反向（`data-platforms-hidden-fallback`），由 JS 在 `applyPlatformFilter()` 末尾根据可见包数判断是否显示。提示文案：`This platform does not include the current page. Choose another platform or return to the index.`
- 默认值：首次访问 `All platforms`；切换后写 localStorage，跨页面跨刷新保留。

## 特性 2：左下角折叠按钮（devsite-book-nav-toggle 风格）

### 现状

- `<button class="ad-book-nav-toggle">‹</button>` 在 nav 末尾，CSS sticky `bottom: 10px`，宽 34×34 浮动方块
- 折叠后 `body.ad-nav-collapsed` 把 nav 收缩到 56px，按钮旋转 180°

### 目标

样式与 Android devsite 的 `devsite-book-nav-toggle` 一致：
- 整条 sticky 底部条占满 nav 宽度，与上方滚动区视觉分离（顶边 1px 实线，底色与 nav 底色一致）
- 按钮内容：SVG 双箭头 (`«`/`»`) + 文本 `Hide navigation` / `Show navigation`
- 折叠后整条 nav 只剩 48px 窄条 + 底部按钮，按钮上 SVG 翻转方向，文本隐藏，但底部条仍贴底显示

### HTML 结构

```html
<nav class="ad-devsite-book-nav" id="ad-book-nav" aria-label="API navigation">
  <div class="ad-book-nav-scroll">
    <!-- 现有 platform selector / nav filter input / 包列表 -->
  </div>
  <div class="ad-book-nav-footer">
    <button class="ad-book-nav-toggle" type="button"
            aria-controls="ad-book-nav" aria-expanded="true">
      <img class="ad-book-nav-toggle-icon" src="<prefix>assets/icon/chevron-double.svg"
           alt="" aria-hidden="true">
      <span class="ad-book-nav-toggle-label">Hide navigation</span>
    </button>
  </div>
</nav>
```

### 关键 CSS

```css
.ad-devsite-book-nav { display: flex; flex-direction: column; }
.ad-book-nav-scroll  { flex: 1 1 auto; overflow: auto; padding: 18px 14px 8px; }
.ad-book-nav-footer  {
  flex: 0 0 auto;
  position: sticky; bottom: 0;
  border-top: 1px solid var(--ad-border);
  background: var(--ad-surface-alt);
  padding: 8px 14px;
}
.ad-book-nav-toggle  {
  display: inline-flex; align-items: center; gap: 7px;
  height: 30px; padding: 0 8px;
  border: 0; background: transparent;
  color: var(--ad-muted); font: 500 12px/1 var(--ad-font-sans);
  cursor: pointer; border-radius: var(--ad-radius-sm);
}
.ad-book-nav-toggle:hover {
  background: var(--ad-surface-strong);
  color: var(--ad-link-hover);
}
.ad-book-nav-toggle-icon { width: 14px; height: 14px; transition: transform .15s ease; }
body.ad-nav-collapsed .ad-book-nav-toggle-icon { transform: rotate(180deg); }
body.ad-nav-collapsed .ad-book-nav-toggle-label { display: none; }
body.ad-nav-collapsed .ad-book-nav-scroll       { display: none; }
body.ad-nav-collapsed { --ad-left-nav-width: 48px; }
body.ad-nav-collapsed .ad-book-nav-footer { padding: 8px 6px; justify-content: center; }
```

### JS 行为

```js
const toggle = document.querySelector(".ad-book-nav-toggle");
const label  = toggle?.querySelector(".ad-book-nav-toggle-label");
const collapsed = localStorage.getItem("apidoc.navCollapsed") === "true";
applyCollapsed(collapsed);
toggle?.addEventListener("click", () => {
  const next = !document.body.classList.contains("ad-nav-collapsed");
  applyCollapsed(next);
  localStorage.setItem("apidoc.navCollapsed", next ? "true" : "false");
});
function applyCollapsed(state) {
  document.body.classList.toggle("ad-nav-collapsed", state);
  toggle?.setAttribute("aria-expanded", state ? "false" : "true");
  if (label) label.textContent = state ? "Show navigation" : "Hide navigation";
}
```

### 资源新增

`core/src/main/resources/apidoc-v2/assets/icon/chevron-double.svg`：双 V 字左箭头（`«`），CSS 旋转 180° 表示展开方向。

## 特性 3：包导航两级展开

### 目标行为

- 点击包名 `<summary>` → 展开包；展开后看到：包的 Overview 链接 + 4 个类别 `<details>`（Interfaces / Classes / Enums / Exceptions / Annotations / Records，仅渲染存在的）
- 类别本身默认折叠；点击类别 `<summary>` → 展开；显示该类别下的 type `<a>` 列表
- 当前页所属包 + 当前页所属类别在初始 HTML 输出时就 `open`
- 当前类型 `<a>` 加 `is-current`，蓝底蓝字
- 当 `#ad-nav-filter` 输入文字时，匹配到的包和类别强制 `open=true`，匹配的 type 保持可见，其它隐藏；清空输入恢复初始 open 状态

### HTML 结构

```html
<details class="ad-book-section ad-package" data-platforms="...">
  <summary class="ad-book-package ad-nav-item" data-platforms="...">
    <span class="ad-package-disclosure">
      <img src="<prefix>assets/icon/chevron.svg" alt="" aria-hidden="true">
    </span>
    <span class="ad-package-name">com.example.foo</span>
    <a class="ad-package-link" href="<prefix>package/com.example.foo.html"
       aria-label="Open package com.example.foo">
      <img src="<prefix>assets/icon/package.svg" alt="" aria-hidden="true">
    </a>
    <span class="ad-package-count">8</span>
  </summary>

  <a class="ad-book-overview ad-nav-item"
     data-platforms="..." data-filter-text="com.example.foo Overview"
     href="<prefix>package/com.example.foo.html">
    <img class="ad-kind-icon" src="<prefix>assets/icon/package.svg" alt="" aria-hidden="true">
    <span>Overview</span>
  </a>

  <details class="ad-package-group" data-group-kind="interfaces" data-platforms="...">
    <summary class="ad-book-group ad-nav-item" data-platforms="...">
      <span class="ad-package-disclosure">
        <img src="<prefix>assets/icon/chevron.svg" alt="" aria-hidden="true">
      </span>
      <span class="ad-group-label">Interfaces</span>
      <span class="ad-package-count">2</span>
    </summary>
    <a class="ad-book-type ad-nav-item" data-platforms="..."
       data-filter-text="com.example.foo Interfaces MyApi"
       href="<prefix>reference/com.example.foo.MyApi.html">
      <img class="ad-kind-icon" src="<prefix>assets/icon/interface.svg" alt="" aria-hidden="true">
      <span>MyApi</span>
    </a>
    ...
  </details>

  <details class="ad-package-group" data-group-kind="classes" ...>...</details>
  <details class="ad-package-group" data-group-kind="enums" ...>...</details>
  <details class="ad-package-group" data-group-kind="exceptions" ...>...</details>
</details>
```

### CSS 变化

```css
.ad-package-group { margin: 0; padding: 0; }
.ad-package-group > summary {
  list-style: none;
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px;
  padding: 5px 8px 5px 22px;
  color: var(--ad-muted);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0;
  text-transform: uppercase;
  cursor: pointer;
}
.ad-package-group > summary::-webkit-details-marker { display: none; }
.ad-package-group[open] > summary .ad-package-disclosure { transform: rotate(90deg); }
.ad-package-group .ad-book-type { padding-left: 44px; }
.ad-book-overview { padding-left: 24px; }
```

第二层缩进比类型行少一级（22px vs 44px），让用户一眼看出层级。

### `bookNav()` 渲染逻辑（伪 Groovy）

```groovy
private static String bookNav(RenderContext context, String prefix, String currentUrl) {
  StringBuilder out = new StringBuilder()
  out << topLink("packages.html", "Packages", "package", currentUrl, prefix)
  out << topLink("classes.html",  "Classes",  "type",    currentUrl, prefix)
  context.projection.nav.each { NavNode pkg ->
    boolean pkgOpen = isOnDescendantUrl(pkg, currentUrl)
    int totalTypes  = countAllTypes(pkg)
    out << "<details class=\"ad-book-section ad-package\"${platformData(pkg.platforms)}${pkgOpen ? ' open' : ''}>"
    out << packageSummaryHtml(pkg, totalTypes, prefix, currentUrl)
    pkg.children.each { NavNode group ->
      if (group.kind == NavNodeKind.OVERVIEW) {
        out << overviewLinkHtml(pkg, group, prefix, currentUrl)
      } else {
        boolean groupOpen = group.children.any { NavNode t -> currentUrl == t.url }
        int typeCount = group.children.size()
        out << "<details class=\"ad-package-group\" data-group-kind=\"${kindKey(group.label)}\"${platformData(group.platforms)}${groupOpen ? ' open' : ''}>"
        out << groupSummaryHtml(group, typeCount)
        group.children.each { NavNode type ->
          out << typeLinkHtml(pkg, group, type, prefix, currentUrl)
        }
        out << "</details>"
      }
    }
    out << "</details>"
  }
  return out.toString()
}
```

辅助 `isOnDescendantUrl(pkg, currentUrl)` 判断当前页是否落在该包的 Overview / 任一 type 上。

### JS 行为变化

`applyNavFilter()` 在新结构下：

```js
function applyNavFilter() {
  if (!navFilter || !nav) return;
  const query = navFilter.value.trim().toLowerCase();
  const platform = currentPlatform();
  nav.querySelectorAll(".ad-package").forEach(pkg => {
    const onPlatform = supportsPlatform(pkg, platform);
    if (!onPlatform) { pkg.hidden = true; return; }
    let visibleTypeCount = 0;
    pkg.querySelectorAll(":scope > .ad-package-group").forEach(group => {
      const groupOnPlatform = supportsPlatform(group, platform);
      if (!groupOnPlatform) { group.hidden = true; return; }
      let groupVisible = 0;
      group.querySelectorAll(":scope > .ad-book-type").forEach(link => {
        const text = (link.dataset.filterText || link.textContent).toLowerCase();
        const matches = !query || text.includes(query);
        const visible = supportsPlatform(link, platform) && matches;
        link.hidden = !visible;
        if (visible) groupVisible++;
      });
      group.hidden = groupVisible === 0;
      if (query && groupVisible) group.open = true;
      // 无查询时不覆盖 group.open，保留用户在本次会话中的手动展开/折叠状态
      const countEl = group.querySelector(":scope > summary .ad-package-count");
      if (countEl) countEl.textContent = String(groupVisible);
      visibleTypeCount += groupVisible;
    });
    const overview = pkg.querySelector(":scope > .ad-book-overview");
    if (overview) {
      const ovVisible = supportsPlatform(overview, platform);
      overview.hidden = !ovVisible;
    }
    pkg.hidden = visibleTypeCount === 0 && !pkg.querySelector(":scope > .ad-book-overview:not([hidden])");
    if (query && !pkg.hidden) pkg.open = true;
    const pkgCountEl = pkg.querySelector(":scope > summary .ad-package-count");
    if (pkgCountEl) pkgCountEl.textContent = String(visibleTypeCount);
  });
}
```

## 特性 4：链接 hover 全局变绿

仅 CSS 改动，模板与 JS 不变。已有变量 `--ad-link-hover: #0b8043`，新增浅绿背景变量。

```css
:root {
  --ad-link-hover: #0b8043;
  --ad-link-hover-soft: #ecf7ee;
}

.ad-book-link:hover, .ad-book-link:focus,
.ad-book-package:hover, .ad-book-package:focus,
.ad-book-type:hover, .ad-book-type:focus,
.ad-book-overview:hover, .ad-book-overview:focus,
.ad-book-group.ad-nav-item:hover, .ad-book-group.ad-nav-item:focus {
  background: var(--ad-surface-strong);
  color: var(--ad-link-hover);
  text-decoration: none;
}

.ad-devsite-toc a:hover, .ad-devsite-toc a:focus {
  border-left-color: var(--ad-link-hover);
  color: var(--ad-link-hover);
  text-decoration: none;
}
@media (min-width: 1121px) {
  .ad-devsite-toc a:hover, .ad-devsite-toc a:focus {
    background: var(--ad-link-hover-soft);
    color: var(--ad-link-hover);
  }
}

.ad-search-result:hover, .ad-search-result:focus {
  background: var(--ad-link-hover-soft);
  color: var(--ad-link-hover);
}
.ad-search-result:hover strong, .ad-search-result:focus strong {
  color: var(--ad-link-hover);
}

.ad-breadcrumbs a:hover, .ad-breadcrumbs a:focus {
  color: var(--ad-link-hover);
  text-decoration: underline;
}

.ad-api-index a:hover, .ad-api-index a:focus,
.ad-index-row a:hover,  .ad-index-row a:focus {
  color: var(--ad-link-hover);
  text-decoration: underline;
}
```

保留蓝色的状态色：

- `.is-current` 仍蓝底蓝字（区分「当前」与「悬停」）
- `.ad-devsite-toc a.is-active` 仍蓝色（当前可视章节）
- `:focus-visible` outline 仍 `--ad-primary`（键盘可达性反馈，与 hover 区分）

## 测试

### 单元/快照测试（沿用 `core/src/test/.../render/HtmlDevsiteRendererTest.groovy` 模式）

| 用例 | 关键断言 |
|---|---|
| 嵌套 nav 渲染 | 输出包含 `<details class="ad-package-group" data-group-kind="..."` 与 `<summary class="ad-book-group ad-nav-item">` |
| 当前类型页所属类别 open | 在路径上的包和类别 `<details>` 渲染了 `open`，其它不渲染 |
| 空 kind 不渲染 | 一个仅含 interface 的包不输出 `data-group-kind="classes"` group |
| nav footer 输出 | 渲染 `<div class="ad-book-nav-footer">` + `<button class="ad-book-nav-toggle">` + `chevron-double.svg` |
| nav 滚动容器 | `<div class="ad-book-nav-scroll">` 包裹 platform selector / filter / 包列表 |
| 平台聚合到 group | group `<details>` 与 `<summary>` 都带 `data-platforms` 等于 type 平台并集 |
| 资源清单 | `HtmlAssetWriter` 写出 `chevron-double.svg`（icon 目录新增） |

### 测试夹具扩展（`core/src/test/resources` 下样例工程）

- 新增一个仅含 `@Supported(platforms={"DiLink300"})` 的接口，验证：(a) 选中 `DiLink300F` 时该接口被隐藏，(b) 选中 `DiLink300` 时显示
- 新增一个空异常 kind 的包（只有 class），验证空 group 不渲染

### 手动 QA（offline，按 v2-android-devsite-target.md 的离线契约）

1. `./gradlew :core:test` 全绿
2. 在样例工程运行生成命令（参见 `docs/portable-usage.md`）
3. `python -m http.server 8080` 启动后用 Chrome 打开
   - 平台下拉切到每个值（含项目里实际声明的 `DiLink300/DiLink300F/DiLink300VCP/DiLinkF_300VCP`）→ 各层级（左 nav、packages.html、classes.html、package-summary、类页面成员表、搜索结果）都正确隐藏
   - 包默认折叠；点击包名展开第一层；点击类别展开第二层
   - 直接打开类型页 → 该包 + 该类别已展开，当前 type 行高亮
   - 左下角 sticky bar 折叠/展开按钮工作；刷新后状态保留；折叠时 nav 收成 48px 窄条
   - Hover：左侧导航包名/类别/类型/Overview、右侧 TOC、搜索结果、面包屑、正文链接，**全部变绿**
   - 当前页（`.is-current`）和当前章节（TOC `.is-active`）保持蓝色状态色
4. 复制输出目录到另一台机器，重复 3 项关键检查（offline 契约）

## 风险与缓解

- **嵌套 `<details>` 可访问性**：`<summary>` 已自带 `button` 角色与键盘支持，无需额外 ARIA。所有交互元素带 `aria-controls` 与可读 `aria-label`。
- **48px 窄条响应式**：现有 `@media (max-width: 760px)` 已把 `.ad-devsite-book-nav` 改为 `display:none`，移动端通过顶部 `.ad-devsite-nav-toggle` 抽屉显示。`ad-nav-collapsed` 仅在桌面端生效。
- **Count 闪烁**：JS 在 `applyPlatformFilter()` 末尾统一调用 `applyNavFilter()`，由 `applyNavFilter()` 完成 count 重算，避免双次 reflow。初始页面加载完成前 HTML 中的 count 是后端预渲染的全量值，与 JS 重算的值一致（无平台筛选时）。
- **`<details>` 的 `open` 持久化**：本设计里我们只用 `open` 表达初始状态（路径展开），不持久化用户手动展开折叠 group 的状态。关闭/重新打开页面时回到默认展开规则。这与 Android devsite 的行为一致。

## 不在范围

- 移动端抽屉行为不变（已实现）
- 搜索后端、search-index 结构不变
- API 状态 chip / inherited members 渲染不变
- v1 HTML 输出和 Markdown 输出不变
