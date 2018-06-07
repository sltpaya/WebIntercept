package com.sltpaya.web.annoation;

public enum PathMode {

    NORMAL,

    /**
     * 模式： 将数组中的path地址全部克隆一份，同时将新克隆的地址的Host替换为指定的部分
     *
     */
    CLONE_REPLACE_HOST,

    /**
     * 模式：将数组中的path地址全部克隆一份，同时在新克隆的地址的Host前添加前缀
     */
    CLONE_ADD_PREFIX


}
