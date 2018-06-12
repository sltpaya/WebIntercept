package com.sltpaya.webintercept;

public class Skin {

    private String name = "skin";

    private static Skin INSTANCE;

    public static Skin getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Skin();
        }
        return INSTANCE;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
