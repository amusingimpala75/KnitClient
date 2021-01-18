package com.github.amusingimpala.knitClient.classLoader;

import com.github.amusingimpala.knitClient.KnitClient;
import com.github.amusingimpala.knitClient.transformations.ClassTransformation;
import com.github.amusingimpala.knitClient.transformations.KnitClassWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KnitClassLoader extends ClassLoader {

    private Map<String, List<ClassTransformation>> transformations = new HashMap<>();

    @Override
    public Class<?> findClass(String name) {
        System.out.println("Using findClass");
        byte[] b = loadClassFromFile(name);
        if (transformations.containsKey(name)) {

        }
        if (KnitClient.devEnv && name.equals("com.github.amusingimpala.knitClient.tests.ToEnum")) {
            System.out.println("Modifying ToEnum enum");
            KnitClassWriter kcw;
            kcw = new KnitClassWriter("com/github/amusingimpala/knitClient/tests/ToEnum");
            //b = kcw.addEnumValues("com/github/amusingimpala/knitClient/tests/FromEnum", "Ljava/lang/String;").write();
        }
        return defineClass(name, b, 0, b.length);
    }

    private byte[] loadClassFromFile(String fileName)  {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
                fileName.replace('.', File.separatorChar) + ".class");
        byte[] buffer;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        int nextValue = 0;
        try {
            while ( (nextValue = inputStream.read()) != -1 ) {
                byteStream.write(nextValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer = byteStream.toByteArray();
        return buffer;
    }

    /*protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {

        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                long t0 = System.nanoTime();
                try {
                    ClassLoader parent = getParent();

                    if (name.equals("com.github.amusingimpala.knitClient.tests.ToEnum")) {
                        c = findClass(name);
                    } else if (parent != null) {
                        Method load = ClassLoader.class.getDeclaredMethod("loadClass", String.class, boolean.class);
                        load.setAccessible(true);
                        c = (Class<?>) load.invoke(parent, name, false);
                    } else {
                        Method bootstrap = ClassLoader.class.getDeclaredMethod("findBootstrapClassOrNull", String.class);
                        bootstrap.setAccessible(true);
                        c = (Class<?>) bootstrap.invoke(parent, name);
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException  e) {
                    e.printStackTrace();
                }

                if (c == null) {
                    // If still not found, then invoke findClass in order
                    // to find the class.
                    c = findClass(name);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }*/
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);

            // FIXME: remove the GSON exclusion once loader stops using gson.
            // We now repackage Gson's JsonReader so removal is now possible
            if (c == null && name.equals("com.github.amusingimpala.knitClient.tests.ToEnum")) {
                byte[] input = getTransformedClass();
                if (input != null) {

                    int pkgDelimiterPos = name.lastIndexOf('.');
                    if (pkgDelimiterPos > 0) {
                        // TODO: package definition stub
                        String pkgString = name.substring(0, pkgDelimiterPos);
                        if (getPackage(pkgString) == null) {
                            definePackage(pkgString, null, null, null, null, null, null, null);
                        }
                    }

                    c = defineClass(name, input, 0, input.length);
                }
            }
            if (c == null) {
                c = super.loadClass(name, resolve);
            }

            if (resolve) {
                resolveClass(c);
            }

            return c;
        }
    }

    public void addTransformation(String clazz, ClassTransformation transformation) {
        if (this.transformations.containsKey(clazz)) {
            this.transformations.get(clazz).add(transformation);
        } else {
            ArrayList<ClassTransformation> list = new ArrayList<>();
            list.add(transformation);
            this.transformations.put(clazz, list);
        }
    }

    public boolean hasTransformation(String name) {
        for (Map.Entry<String, List<ClassTransformation>> entry : transformations.entrySet()) {
            if (entry.getKey().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public byte[] getTransformedClass() {
        KnitClassWriter kcw;
        kcw = new KnitClassWriter("com/github/amusingimpala/knitClient/tests/ToEnum");
        System.out.println("getting transformed ToEnum class");
        return kcw.addEnumValues("com/github/amusingimpala/knitClient/tests/FromEnum", "Ljava/lang/String;");
    }
}
