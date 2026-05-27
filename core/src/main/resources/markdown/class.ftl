## ${doc.name}

<#if doc.packageName??>
**包：** `${doc.packageName}`

</#if>
<#if doc.desc?has_content>
**描述：** ${data.typeLinker.linkifyDescription(doc.desc, docPath)}

</#if>

<#if doc.superClass?? || (doc.interfaces?? && (doc.interfaces?size > 0))>
**继承关系：**
<#if doc.superClass??>- **父类：** `${doc.superClass}`
</#if>
<#if doc.interfaces?? && (doc.interfaces?size > 0)>
- **实现的接口：** <#list doc.interfaces as i>${data.typeLinker.linkifyGeneric(i, docPath)}<#if i_has_next>, </#if></#list>
</#if>

</#if>

<#if doc.annotations?? && (doc.annotations?size > 0)>
**类注解：**
<#list doc.annotations as ann>- `${data.typeLinker.linkifyGeneric(ann, docPath)}`
</#list>

</#if>

<#-- 枚举常量 -->
<#if doc.enumConstants?? && (doc.enumConstants?size > 0)>
## 枚举常量
| 常量 | 值 | 描述 |
|------|----|------|
<#list doc.enumConstants as c>
| ${utils.escapeMarkdownIdentifier(c.name)} | ${utils.escapeMarkdown(c.value!'')} | ${(utils.escapeMarkdownForTableCell(data.typeLinker.linkifyDescriptionForTableCell(c.description!'', docPath)))} |
</#list>
</#if>

<#-- 字段 -->
<#if doc.fields?? && (doc.fields?size > 0)>
## 字段概览
| 修饰符 | 字段名 | 类型 | 描述 |
|--------|--------|------|------|
<#list doc.fields as f>
| ${f.modifier!''} | ${utils.escapeMarkdownIdentifier(f.name)} | ${data.typeLinker.linkifyGeneric(f.type!"", docPath!"")} | ${(utils.escapeMarkdownForTableCell(data.typeLinker.linkifyDescriptionForTableCell(f.description!'', docPath)))} |
</#list>
</#if>

<#-- 构造函数 -->
<#if doc.constructors?? && (doc.constructors?size > 0)>
## 构造函数概览
| 修饰符 | 构造函数签名 | 描述 |
|--------|--------------|------|
<#list doc.constructors as m>
<#assign ctorAnchor = doc.name?lower_case?replace("[^a-z0-9\\s]", "")?replace("\\s+", "-")?replace("-$", "")>
| ${m.modifiers!''} | [${utils.escapeMarkdownIdentifier(doc.name)}](#${ctorAnchor}) | ${data.typeLinker.linkifyDescription(m.desc!'', docPath)} |
</#list>
</#if>

<#-- 方法 -->
<#if doc.list?? && (doc.list?size > 0)>
## 方法概览
| 修饰符和类型 | 方法 | 描述 |
|--------------|------|------|
<#list doc.list as m>
<#assign methodAnchor = m.name?lower_case?replace("[^a-z0-9\\s]", "")?replace("\\s+", "-")?replace("-$", "")>
| <#if m.modifiers?has_content>${m.modifiers} ${data.typeLinker.linkifyGeneric(m.returnType!"", docPath!"")}<#else>${data.typeLinker.linkifyGeneric(m.returnType!"", docPath!"")}</#if> | [${utils.escapeMarkdownIdentifier(m.name)}](#${methodAnchor}) | ${(utils.escapeMarkdownForTableCell(data.typeLinker.linkifyDescriptionForTableCell(m.desc!'', docPath)))} |
</#list>

## 方法详情
<#list doc.list as m>
<#assign methodAnchor = m.name?lower_case?replace("[^a-z0-9\\s]", "")?replace("\\s+", "-")?replace("-$", "")>
### ${m.name}

**签名：** ${data.typeLinker.linkifyGeneric(m.url!"", docPath!"")}

<#if m.modifiers?has_content>
**修饰符：** ${m.modifiers}

</#if>
<#if m.returnComment?has_content>
**返回值：** ${data.typeLinker.linkifyDescription(m.returnComment, docPath)}

<#else>
**返回值类型：** ${data.typeLinker.linkifyGeneric(m.returnType!"", docPath!"")}

</#if>

<#if m.parameters?? && (m.parameters?size > 0)>
**参数：**

| 参数名 | 类型 | 描述 |
|--------|------|------|
<#list m.parameters as p>
| ${utils.escapeMarkdownIdentifier(p.name)} | ${data.typeLinker.linkifyGeneric(p.type!"", docPath!"")} | ${(utils.escapeMarkdownForTableCell(data.typeLinker.linkifyDescriptionForTableCell(p.description!'', docPath)))} |
</#list>

</#if>

<#if m.exceptions?? && (m.exceptions?size > 0)>
**抛出异常：**
<#list m.exceptions as ex>- `${ex}`
</#list>

</#if>

<#if m.annotations?? && (m.annotations?size > 0)>
**方法注解：**
<#list m.annotations as ann>- ${data.typeLinker.linkifyGeneric(ann, docPath)}
</#list>

</#if>

</#list>
</#if>