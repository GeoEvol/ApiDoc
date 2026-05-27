package com.byd.apidoc.renderer

import com.byd.apidoc.model.ApiConfig

/**
 * Markdown 渲染器。
 * @author qiao.zhi2
 */
class MarkdownRenderer extends AbstractFreemarkerRenderer {

    @Override
    String getFormat() {
        return ApiConfig.FORMAT_MARKDOWN
    }

    @Override
    protected String getClasspathTemplateRoot() {
        return ApiConfig.TEMPLATE_ROOT_MARKDOWN
    }

    @Override
    protected String getIndexFileName() {
        return "index" + ApiConfig.EXTENSION_MARKDOWN
    }

    @Override
    protected String getFileExtension() {
        return ApiConfig.EXTENSION_MARKDOWN
    }
}