package com.byd.apidoc.model;

import org.gradle.api.tasks.Input; // <-- 添加此导入

import java.util.HashMap;
import java.util.Map;

/**
 * 模板渲染配置模型
 * 控制文档生成过程中的展示逻辑和内容开关。
 * @author qiao.zhi2
 */
public class TemplateConfig {

    /** 是否显示从父类继承的字段和方法 */
    private boolean showInheritedMembers = true;

    /** 是否在方法概览中显示修饰符 (如 public, static) */
    private boolean showMethodModifiers = true;

    /** 是否显示类/方法上的注解 */
    private boolean showAnnotations = true;

    /** 是否为枚举生成“构造参数详情”表格 */
    private boolean showEnumConstructorDetails = true;

    // === 高级自定义 (Map) ===
    /** 用于存储未来可能的、未预定义的配置项 */
    private Map<String, Object> customOptions = new HashMap<>();

    // --- 为所有相关的 getter 方法添加 @Input 注解 ---

    @Input
    public boolean isShowInheritedMembers() {
        return showInheritedMembers;
    }
    public void setShowInheritedMembers(boolean showInheritedMembers) {
        this.showInheritedMembers = showInheritedMembers;
    }

    @Input
    public boolean isShowMethodModifiers() {
        return showMethodModifiers;
    }
    public void setShowMethodModifiers(boolean showMethodModifiers) {
        this.showMethodModifiers = showMethodModifiers;
    }

    @Input
    public boolean isShowAnnotations() {
        return showAnnotations;
    }
    public void setShowAnnotations(boolean showAnnotations) {
        this.showAnnotations = showAnnotations;
    }

    @Input
    public boolean isShowEnumConstructorDetails() {
        return showEnumConstructorDetails;
    }
    public void setShowEnumConstructorDetails(boolean showEnumConstructorDetails) {
        this.showEnumConstructorDetails = showEnumConstructorDetails;
    }

}