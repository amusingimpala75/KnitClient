package com.github.amusingimpala.knitClient.tests;

import com.github.amusingimpala.knitClient.transformations.EnumAdditionProvider;

public enum FromEnum implements EnumAdditionProvider {
    ASM("asm"),
    UNSAFE("unsafe"),
    OBJECT_WEB("object_web"),
    RAND("rand"),
    RAND_1("rand"),
    RAND_2("rand"),
    RAND_3("rand");

    private String name;

    FromEnum(String name) {
        this.name = name;
    }

    public Object[] getParams() {
        return new Object[] {name};
    }
}
