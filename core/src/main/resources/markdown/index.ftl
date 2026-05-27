# ${data.projectName} API 文档

生成时间：${data.generatedAt?string("yyyy-MM-dd HH:mm:ss")}

## 概览
- 类总数：${(data.docStats.classCount)!0}
- 方法总数：${(data.docStats.methodCount)!0}
- 构造函数：${(data.docStats.constructorCount)!0}
- 字段总数：${(data.docStats.fieldCount)!0}
- 枚举常量：${(data.docStats.enumCount)!0}
- 标签总数：${(data.docStats.tagCount)!0}

<#if data.tagIndex?has_content>
## 按标签
| 标签 | 类 |
|------|----|
<#list data.tagIndex?keys as tag>
| ${tag} | <#list data.tagIndex[tag] as c><#if (data.docPathByName[c.fullyQualifiedName])??>[${utils.escapeMarkdown(c.name)}](${data.docPathByName[c.fullyQualifiedName]})<#else>${utils.escapeMarkdown(c.name)}</#if><#if c_has_next>, </#if></#list> |
</#list>
</#if>

<#if data.packageIndex?has_content>
## 按包
| 包 | 类 |
|----|----|
<#list data.packageIndex?keys as pkg>
| ${pkg} | <#list data.packageIndex[pkg] as c><#if (data.docPathByName[c.fullyQualifiedName])??>[${utils.escapeMarkdown(c.name)}](${data.docPathByName[c.fullyQualifiedName]})<#else>${utils.escapeMarkdown(c.name)}</#if><#if c_has_next>, </#if></#list> |
</#list>
</#if>

## 类型分类
<#if data.interfaces?has_content>
### 接口
<#list data.interfaces as c>- <#if (data.docPathByName[c.fullyQualifiedName])??>[${utils.escapeMarkdown(c.name)}](${data.docPathByName[c.fullyQualifiedName]})<#else>${utils.escapeMarkdown(c.name)}</#if> ${(utils.escapeMarkdownForTableCell(data.typeLinker.linkifyWithNewlines(c.desc!'', "")))}
</#list>
</#if>

<#if data.classes?has_content>
### 类
<#list data.classes as c>- <#if (data.docPathByName[c.fullyQualifiedName])??>[${utils.escapeMarkdown(c.name)}](${data.docPathByName[c.fullyQualifiedName]})<#else>${utils.escapeMarkdown(c.name)}</#if> ${(utils.escapeMarkdownForTableCell(data.typeLinker.linkifyWithNewlines(c.desc!'', "")))}
</#list>
</#if>

<#if data.enums?has_content>
### 枚举
<#list data.enums as c>- <#if (data.docPathByName[c.fullyQualifiedName])??>[${utils.escapeMarkdown(c.name)}](${data.docPathByName[c.fullyQualifiedName]})<#else>${utils.escapeMarkdown(c.name)}</#if> ${(utils.escapeMarkdownForTableCell(data.typeLinker.linkifyWithNewlines(c.desc!'', "")))}
</#list>
</#if>

<#if data.annotations?has_content>
### 注解
<#list data.annotations as c>- <#if (data.docPathByName[c.fullyQualifiedName])??>[${utils.escapeMarkdown(c.name)}](${data.docPathByName[c.fullyQualifiedName]})<#else>${utils.escapeMarkdown(c.name)}</#if> ${(utils.escapeMarkdownForTableCell(data.typeLinker.linkifyWithNewlines(c.desc!'', "")))}
</#list>
</#if>

<#if data.exceptions?has_content>
### 异常
<#list data.exceptions as c>- <#if (data.docPathByName[c.fullyQualifiedName])??>[${utils.escapeMarkdown(c.name)}](${data.docPathByName[c.fullyQualifiedName]})<#else>${utils.escapeMarkdown(c.name)}</#if> ${(utils.escapeMarkdownForTableCell(data.typeLinker.linkifyWithNewlines(c.desc!'', "")))}
</#list>
</#if>