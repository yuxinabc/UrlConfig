package com.cassInfo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 公司    云集共享科技
 * 创建时间 2019/2/15
 * 描述     url自动化配置的注解
 *
 * @author zhuxi
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface UrlConfig {
    String value() default "";
}
