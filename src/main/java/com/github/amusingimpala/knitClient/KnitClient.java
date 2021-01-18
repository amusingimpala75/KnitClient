package com.github.amusingimpala.knitClient;

import com.github.amusingimpala.knitClient.classLoader.KnitClassLoader;
import com.github.amusingimpala.knitClient.tests.ToEnum;
import com.github.amusingimpala.knitClient.transformations.KnitClassWriter;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
            kcl.addTransformation("com.github.amusingimpala.knitClient.tests.ToEnum", null);
            try {
                kcl.loadClass("com.github.amusingimpala.knitClient.tests.ToEnum");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            ToEnum[] enumVals = ToEnum.values();
            for (ToEnum val : enumVals) {
                System.out.println(val.getName());
            }
            ToEnum.valueOf("UNSAFE").getName();
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
        kcw = new KnitClassWriter("com/github/amusingimpala/knitClient/tests/ToEnum");
        //bytes = kcw.addEnumValues("com/github/amusingimpala/knitClient/tests/FromEnum", "Ljava/lang/String;").write();
        try {
            Files.write(Paths.get("C:/", "Users", "lukee", "modsForJava", "knitClient", "run", "out", "ToEnum.class"), bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
