package com.sltpaya.web.annoation;

public @interface InterceptGroup {

    /**
     * 拦截器所属组如果此处填写名称，则生成相应的类
     */
    String[] name();

}
