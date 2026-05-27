package com.example.sdk;

/**
 * Sample annotation type.
 */
public @interface SdkAnnotation {
    /**
     * Annotation value.
     *
     * @return value
     */
    String value() default "";
}
