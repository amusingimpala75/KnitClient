package com.github.amusingimpala.knitClient.annotations;

public @interface Target {
    String value() default "";
    String compatPrefix() default "";
}
