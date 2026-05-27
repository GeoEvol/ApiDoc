package com.byd.apidoc.renderer

import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.ApiDoc
import com.byd.apidoc.model.TemplateData
import freemarker.template.Configuration
import freemarker.template.Template

class HtmlRenderer extends AbstractFreemarkerRenderer {

    @Override
    String getFormat() {
        return ApiConfig.FORMAT_HTML
    }

    @Override
    protected String getClasspathTemplateRoot() {
        return ApiConfig.TEMPLATE_ROOT_HTML
    }

    @Override
    protected String getIndexFileName() {
        return "index" + ApiConfig.EXTENSION_HTML
    }

    @Override
    protected String getFileExtension() {
        return ApiConfig.EXTENSION_HTML
    }

    @Override
    void render(TemplateData data, File outputDir) {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        Configuration cfg = getOrCreateConfiguration()
        beforeRender(data, outputDir)

        writeTemplate(cfg.getTemplate(ApiConfig.TEMPLATE_INDEX), createBaseModel(data), new File(outputDir, getIndexFileName()))

        Template classTpl = cfg.getTemplate(ApiConfig.TEMPLATE_CLASS)
        data.apiDocs?.each { ApiDoc doc ->
            String relativePath = data.docPathMap?.get(doc) ?: "${doc.name}${getFileExtension()}"
            writeTemplate(classTpl, createClassModel(data, doc, relativePath), new File(outputDir, relativePath))
        }

        Template packageTpl = cfg.getTemplate("package-summary.ftl")
        data.packageIndex?.each { String pkg, List<ApiDoc> docs ->
            String packagePath = data.packagePathMap[pkg]
            Map<String, Object> model = createBaseModel(data)
            model.put("pkg", pkg)
            model.put("docs", docs)
            model.put("docPath", packagePath)
            writeTemplate(packageTpl, model, new File(outputDir, packagePath))
        }

        writeTemplate(cfg.getTemplate("index-all.ftl"), createBaseModel(data), new File(outputDir, data.indexAllPath))
        writeTemplate(cfg.getTemplate("search.ftl"), createBaseModel(data), new File(outputDir, data.searchPath))

        afterRender(data, outputDir)
    }

    @Override
    protected void afterRender(TemplateData data, File outputDir) {
        logger.info("HTML docs generated: ${outputDir.absolutePath}")
    }
}
