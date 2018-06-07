package com.sltpaya.web.annoation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Intercept {

    /**
     * 需要拦截的网址，支持UriMatcher匹配和正则表达式语法
     */
    String[] path();

    /**
     * 该拦截器的优先级
     * 注意：优先级不支持顺序执行，即优先级高的拦截后，优先级低的不会再继续匹配拦截
     */
    int priority() default 0;

    /**
     * Path模式
     * {@link PathMode#NORMAL}正常模式，拦截的网址不会克隆一份
     * {@link PathMode#CLONE_REPLACE_HOST}正常模式，拦截的网址会克隆一份,并替换Host
     * {@link PathMode#CLONE_ADD_PREFIX}正常模式，拦截的网址会克隆一份，在Host前加上前缀
     */
    PathMode mode() default PathMode.NORMAL;

    /**
     * Path模式为
     * {@link PathMode#CLONE_REPLACE_HOST}有效
     */
    String[] host() default {};

    /**
     * Path模式为
     * {@link PathMode#CLONE_ADD_PREFIX}有效
     */
    String[] prefix() default {};

    /**
     * 匹配模式
     * {@link MatchMode#URI_MATCHER} UriMatcher匹配 默认
     * {@link MatchMode#REGEXP} 正则表达式匹配
     *
     * 如果优先级相同的拦截器，则UriMatcher匹配模式优先，正则表达式匹配靠后
     */
    MatchMode matchMode() default MatchMode.URI_MATCHER;


    /**
     * 仅在正则匹配模式下有效
     * {@link java.util.regex.Pattern#compile(String, int)} 中的flags
     */
    int regExpFlag() default 0;

}
