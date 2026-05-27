package com.byd.apidoc.renderer

import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.ApiDoc
import com.byd.apidoc.model.TemplateData
import com.byd.apidoc.utils.FreeMarkerUtil
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler

import java.util.logging.Logger

/**
 * 抽象渲染器基类。
 * @author qiao.zhi2
 */
abstract class AbstractFreemarkerRenderer implements DocRenderer {

    protected final Logger logger = Logger.getLogger(getClass().getName())

    /**
     * ThreadLocal 缓存 Configuration 对象
     * 同一线程内复用 Configuration，避免重复创建
     */
    private static final ThreadLocal<Configuration> configCache = new ThreadLocal<>()

    /**
     * 返回格式标识，例如 markdown、html
     */
    abstract String getFormat()

    /**
     * 返回类路径下模板资源的根目录，例如 /markdown、/html
     */
    protected abstract String getClasspathTemplateRoot()

    /**
     * 返回首页文件名，例如 index.md、index.html
     */
    protected abstract String getIndexFileName()

    /**
     * 返回类文件的扩展名，例如 .md、.html
     */
    protected abstract String getFileExtension()

    /**
     * 渲染前钩子，子类可以在渲染之前执行额外的操作
     */
    protected void beforeRender(TemplateData data, File outputDir) {
        // 默认空实现
    }

    /**
     * 渲染后钩子，子类可以在渲染之后执行额外的操作（如复制静态资源）
     */
    protected void afterRender(TemplateData data, File outputDir) {
        // 默认空实现
    }

    @Override
    void render(TemplateData data, File outputDir) {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        Configuration cfg = getOrCreateConfiguration()

        beforeRender(data, outputDir)

        // 渲染首页
        Template indexTpl = cfg.getTemplate(ApiConfig.TEMPLATE_INDEX)
        Map<String, Object> indexModel = createBaseModel(data)
        writeTemplate(indexTpl, indexModel, new File(outputDir, getIndexFileName()))

        // 渲染每个类（按包路径落盘）
        Template classTpl = cfg.getTemplate(ApiConfig.TEMPLATE_CLASS)
        data.apiDocs?.each { ApiDoc doc ->
            if (doc.name?.contains('$')) return
            String relativePath = data.docPathMap?.get(doc) ?: "${doc.name}${getFileExtension()}"
            File target = new File(outputDir, relativePath)

            Map<String, Object> classModel = createClassModel(data, doc, relativePath)
            writeTemplate(classTpl, classModel, target)
        }

        afterRender(data, outputDir)
    }

    /**
     * 创建基础模板模型
     */
    protected Map<String, Object> createBaseModel(TemplateData data) {
        Map<String, Object> model = new HashMap<>()
        model.put("data", data)
        model.put("utils", new FreeMarkerUtil())
        return model
    }

    /**
     * 创建类文档的模板模型
     */
    protected Map<String, Object> createClassModel(TemplateData data, ApiDoc doc, String docPath) {
        Map<String, Object> model = createBaseModel(data)
        model.put("doc", doc)
        model.put("docPath", docPath)
        return model
    }

    /**
     * 创建 FreeMarker 配置对象
     * 使用类路径下的默认模板
     */
    protected Configuration createConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31)
        cfg.setDefaultEncoding("UTF-8")
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)
        cfg.setLogTemplateExceptions(false)
        cfg.setWrapUncheckedExceptions(true)
        cfg.setFallbackOnNullLoopVariable(false)
        cfg.setClassLoaderForTemplateLoading(this.class.classLoader, getClasspathTemplateRoot())
        return cfg
    }

    /**
     * 获取或创建 FreeMarker Configuration（带缓存）
     * 同一线程内复用 Configuration
     *
     * @return Configuration 对象
     */
    protected Configuration getOrCreateConfiguration() {
        // 检查缓存是否可用
        Configuration cached = configCache.get()
        if (cached != null) {
            logger.fine("Reusing cached FreeMarker Configuration")
            return cached
        }

        // 创建新的 Configuration
        Configuration cfg = createConfiguration()
        configCache.set(cfg)
        return cfg
    }

    /**
     * 使用模板渲染并写入目标文件
     */
    protected void writeTemplate(Template tpl, Map<String, Object> model, File target) {
        target.parentFile?.mkdirs()
        target.withWriter("UTF-8") { writer ->
            tpl.process(model, writer)
        }
    }
}