package com.github.amusingimpala.knitClient.tests;

import com.github.amusingimpala.knitClient.transformations.EnumAdditionProvider;

public enum ToEnum implements EnumAdditionProvider {
    JAVA("java"),
    SAFE("safe"),
    OPEN_JDK("open_jdk");

    private final String name;

    ToEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Object[] getParams() {
        return new Object[]{name};
    }
}
