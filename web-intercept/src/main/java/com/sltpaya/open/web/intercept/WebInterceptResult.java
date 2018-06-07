package com.sltpaya.open.web.intercept;

public class WebInterceptResult {

    /**
     * 表示处理了并开启了新界面（Activity)
     */
    public static final int ACTION = 0;

    /**
     * 表示处理了但是没有开启新界面
     */
    public static final int NO_ACTION = 1;

    /**
     * 表示未被处理
     */
    public static final int NORMAL = -1;

    /**
     * 处理结果的类型
     * {@link #ACTION} 表示处理了并开启了新界面（Activity)
     * {@link #NO_ACTION} 表示处理了但是没有开启新界面
     * {@link #NORMAL} 表示未被处理
     */
    private int type = NORMAL;

    /**
     * 额外的对象
     */
    public Object extra;

    /**
     * 内部变量，不对外提供
     */
    private boolean isHandle = false;

    private static WebInterceptResult normalInstance;

    private WebInterceptResult() {}

    /**
     * 创建类型为{@link #ACTION}的WebInterceptResult对象
     * @return WebInterceptResult {@link WebInterceptResult}
     */
    public static WebInterceptResult action() {
        WebInterceptResult webInterceptResult = new WebInterceptResult();
        webInterceptResult.type = ACTION;
        webInterceptResult.isHandle = true;
        return webInterceptResult;
    }

    /**
     * 创建类型为{@link #NO_ACTION}的WebInterceptResult对象
     * @return WebInterceptResult {@link WebInterceptResult}
     */
    public static WebInterceptResult noAction() {
        WebInterceptResult webInterceptResult = new WebInterceptResult();
        webInterceptResult.type = NO_ACTION;
        webInterceptResult.isHandle = true;
        return webInterceptResult;
    }

    /**
     * 创建的无处理WebInterceptResult对象
     * @return WebInterceptResult {@link WebInterceptResult}
     */
    public static WebInterceptResult normal() {
        if (normalInstance == null) {
            normalInstance = new WebInterceptResult();
        }
        normalInstance.type = NORMAL;
        normalInstance.isHandle = false;
        return normalInstance;
    }

    public int type() {
        return type;
    }

    boolean isHandle() {
        return isHandle;
    }

}
