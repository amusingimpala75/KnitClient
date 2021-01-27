package com.github.amusingimpala.knitClient;

import com.github.amusingimpala.knitClient.classLoader.KnitClassLoader;
import com.github.amusingimpala.knitClient.transformations.KnitClassTransformer;
import com.github.amusingimpala.knitClient.transformations.KnitClassWriter;
import com.github.amusingimpala.knitClient.transformations.KnitClassWriterV2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class KnitClient {

    public static final boolean devEnv = true;

    public static void main(String[] args) {
        boolean syntaxError = false;
        String secondMainClass;
        if (devEnv) {
        } else {
            secondMainClass = args[0];
        }
        String modificationDir;

        if (devEnv) {

        } else {
            modificationDir = args[1];
        }

        if (syntaxError) {
            System.out.println("Error in run syntax! Should be in form of:");
            System.out.println("arg 1 - path/separated/by/slashes/to/main/jar.plus.internal.path.separated.by.periods.to.Main.Class");
        }

        KnitClassLoader kcl = new KnitClassLoader();

        Thread.currentThread().setContextClassLoader(kcl);

        if (devEnv) {
            runTests();
        }
    }

    public static void runTests() {
        KnitClassWriter kcw = new KnitClassWriter("java/lang/String");
        byte[] bytes = kcw.widenField("value").write();
        try {
            Files.write(Paths.get("C:/", "Users", "lukee", "modsForJava", "knitClient", "run", "out", "String.class"), bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*kcw = new KnitClassWriter("com/github/amusingimpala/knitClient/tests/ToEnum");
        bytes = kcw
                .addEnumValues("com/github/amusingimpala/knitClient/tests/FromEnum", "Ljava/lang/String;")
                .addInterface("java/lang/Cloneable")
                .widenField("name")
                .write();
        try {
            Files.write(Paths.get("C:/", "Users", "lukee", "modsForJava", "knitClient", "run", "out", "ToEnum.class"), bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        KnitClassWriterV2 kcwv2 ;//= new KnitClassWriterV2("com/github/amusingimpala/knitClient/tests/TestClass");
        KnitClassTransformer kct = new KnitClassTransformer().init("com/github/amusingimpala/knitClient/tests/ModifierClass");
        //kcwv2.addInterface("java/lang/Cloneable", Optional.empty());
        //kcwv2.widenFieldAccess("name", true, ACC_PUBLIC);
        //kcwv2.widenClassAccess(ACC_PUBLIC, true);
        //kcwv2.widenMethodAccess("doSomething", "()V", ACC_PUBLIC, true);
        try {
            Files.write(Paths.get("C:/", "Users", "lukee", "modsForJava", "knitClient", "run", "out", "TestClass.class"), kct.processClass("com/github/amusingimpala/knitClient/tests/ModifierClass").write());
        } catch (IOException e) {
            e.printStackTrace();
        }

        kcwv2 = new KnitClassWriterV2("com/github/amusingimpala/knitClient/tests/ToEnum");
        kcwv2.tryAddInterface("java/lang/Cloneable", Optional.empty());
        //kcwv2.addEnumEntries("com/github/amusingimpala/knitClient/tests/FromEnum", "Ljava/lang/String;", "test");
        try {
            Files.write(Paths.get("C:/", "Users", "lukee", "modsForJava", "knitClient", "run", "out", "ToEnum.class"), kcwv2.write());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
