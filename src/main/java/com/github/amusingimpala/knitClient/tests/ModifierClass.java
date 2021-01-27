package com.github.amusingimpala.knitClient.tests;

import com.github.amusingimpala.knitClient.annotations.*;
import org.objectweb.asm.Opcodes;

@Target(value = "com.github.amusingimpala.knitClient.tests.TestClass", compatPrefix = "test")
@Widen(value = Opcodes.ACC_PUBLIC, definalize = true)
public class ModifierClass implements Cloneable {

    @Shadow
    @Widen(value = Opcodes.ACC_PUBLIC, definalize = true)
    @Accessor(setter = true, getter = true)
    public String name;

    @Shadow
    @Widen(value = Opcodes.ACC_PUBLIC, definalize = true)
    @Invoker
    public void doSomething() {
        throw new AssertionError();
    }

    private int thing = 2;

    private void toBeTransferred() {
        System.out.println("I wanna move!");
    }

}
