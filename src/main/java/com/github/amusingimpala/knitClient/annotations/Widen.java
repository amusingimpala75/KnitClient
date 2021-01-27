package com.github.amusingimpala.knitClient.annotations;

import org.objectweb.asm.Opcodes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
public @interface Widen {
    int value() default Opcodes.ACC_PROTECTED;
    boolean definalize() default false;
}
