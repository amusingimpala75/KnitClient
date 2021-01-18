package com.github.amusingimpala.knitClient.tests;

public enum ToEnum {
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
}
