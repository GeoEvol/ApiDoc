package com.example.sdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sample permission annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresPermission {
    /**
     * Required permission name.
     *
     * @return permission name
     */
    String[] value() default {};

    /**
     * Permissions that are all required.
     *
     * @return permission names
     */
    String[] allOf() default {};

    /**
     * Permissions where any one is required.
     *
     * @return permission names
     */
    String[] anyOf() default {};
}
