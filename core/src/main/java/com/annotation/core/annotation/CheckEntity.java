package com.annotation.core.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface CheckEntity {
    Class<?> entity();

    Class<?> dto();

    String[] excludeFields() default {};
}