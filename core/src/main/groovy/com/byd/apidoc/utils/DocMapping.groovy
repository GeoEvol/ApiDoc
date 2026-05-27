package com.byd.apidoc.utils

import com.byd.apidoc.model.ApiDoc
import com.byd.apidoc.model.ApiMethodDoc
import com.byd.apidoc.model.TagDoc

import java.util.Collections
import java.util.HashSet

/**
 * 文档映射管理器
 * 管理标签与文档元素（类、方法）之间的关联关系
 * @author qiao.zhi2
 */
class DocMapping {

    /**
     * 标签到 TagDoc 的映射表
     * 使用 LinkedHashMap 保证顺序
     */
    private final Map<String, TagDoc> tagDocMap = new LinkedHashMap<>()

    /**
     * 延迟构建的反向索引：类到标签的映射
     * 在首次查询时构建，避免实时维护的数据一致性问题
     */
    private Map<ApiDoc, Set<TagDoc>> classToTags = null

    /**
     * 延迟构建的反向索引：方法到标签的映射
     * 在首次查询时构建，避免实时维护的数据一致性问题
     */
    private Map<ApiMethodDoc, Set<TagDoc>> methodToTags = null

    /**
     * 注册一个标签与文档元素的关联。
     * 如果指定的标签不存在，会自动创建一个新的 TagDoc 对象。
     *
     * @param tag         自定义的 @tag 标签值
     * @param clazzDoc    关联的类文档对象，可为 null
     * @param methodDoc   关联的方法文档对象，可为 null
     */
    void tagDocPut(String tag, ApiDoc clazzDoc, ApiMethodDoc methodDoc) {
        if (tag == null || tag.trim().isEmpty()) {
            return
        }
        String normalizedTag = tag.trim()
        TagDoc tagDoc = tagDocMap.computeIfAbsent(normalizedTag) { new TagDoc(normalizedTag) }

        if (clazzDoc != null) {
            tagDoc.getClazzDocs().add(clazzDoc)
        }
        if (methodDoc != null) {
            tagDoc.getMethodDocs().add(methodDoc)
        }

        // 标记反向索引为脏，下次查询时重建
        classToTags = null
        methodToTags = null
    }

    /**
     * 获取所有已注册的 TagDoc 集合。
     * 通常在解析完成后，用于构建 ApiDoc 的 tagRefs。
     *
     * @return 所有 TagDoc 的集合视图
     */
    Collection<TagDoc> getAllTagDocs() {
        return tagDocMap.values()
    }

    /**
     * 根据标签名获取对应的 TagDoc。
     *
     * @param tag 标签名
     * @return 对应的 TagDoc，如果不存在则返回 null
     */
    TagDoc getTagDoc(String tag) {
        return tagDocMap.get(tag)
    }

    /**
     * 构建反向索引（延迟构建，只在首次查询时调用）
     * 从正向映射 tagDocMap 构建 classToTags 和 methodToTags
     */
    private void buildReverseIndex() {
        // 已构建则跳过
        if (classToTags != null) {
            return
        }

        classToTags = new HashMap<>()
        methodToTags = new HashMap<>()

        for (TagDoc tagDoc : tagDocMap.values()) {
            // 构建类到标签的反向索引
            for (ApiDoc doc : tagDoc.getClazzDocs()) {
                classToTags.computeIfAbsent(doc, { new HashSet<TagDoc>() }).add(tagDoc)
            }
            // 构建方法到标签的反向索引
            for (ApiMethodDoc method : tagDoc.getMethodDocs()) {
                methodToTags.computeIfAbsent(method, { new HashSet<TagDoc>() }).add(tagDoc)
            }
        }
    }

    /**
     * 获取指定类关联的所有标签（O(1) 复杂度）
     *
     * @param apiDoc 类文档对象
     * @return 关联的标签集合，如果无关联则返回空集合
     */
    Set<TagDoc> getTagsForClass(ApiDoc apiDoc) {
        if (apiDoc == null) {
            return Collections.emptySet()
        }
        buildReverseIndex()
        return classToTags.getOrDefault(apiDoc, Collections.emptySet())
    }

    /**
     * 获取指定方法关联的所有标签（O(1) 复杂度）
     *
     * @param methodDoc 方法文档对象
     * @return 关联的标签集合，如果无关联则返回空集合
     */
    Set<TagDoc> getTagsForMethod(ApiMethodDoc methodDoc) {
        if (methodDoc == null) {
            return Collections.emptySet()
        }
        buildReverseIndex()
        return methodToTags.getOrDefault(methodDoc, Collections.emptySet())
    }
}