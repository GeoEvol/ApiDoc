package com.byd.apidoc.renderer

import com.byd.apidoc.model.TemplateData

/**
 * 文档渲染器接口，支持多格式输出。
 */
interface DocRenderer {

    /**
     * 返回渲染格式标识，例如 markdown、html。
     */
    String getFormat()

    /**
     * 根据模板数据生成文档到指定目录。
     *
     * @param data 渲染所需的模板数据
     * @param outputDir 输出目录
     */
    void render(TemplateData data, File outputDir)
}