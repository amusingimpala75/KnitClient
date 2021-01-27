package com.github.amusingimpala.knitClient.transformations;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Optional;

public class KnitClassTransformer {

    private ClassNode clazz;
    private KnitClassWriterV2 kcw;

    public KnitClassTransformer processClass(String clazz) {

        this.clazz = KnitClassWriterV2.getNode(clazz);

        //Widen Class
        for (AnnotationNode an : this.clazz.invisibleAnnotations) {
            System.out.println(an.desc);
            if (an.desc.equals("Lcom/github/amusingimpala/knitClient/annotations/Widen;")) {
                kcw.widenClassAccess((Integer) an.values.get(1), (Boolean) an.values.get(3));
            }
        }

        //Widen Fields
        for (FieldNode fn : this.clazz.fields) {
            if (fn.invisibleAnnotations == null) {
                continue;
            }
            for (AnnotationNode an : fn.invisibleAnnotations) {
                System.out.println(an.desc);
                if (an.desc.equals("Lcom/github/amusingimpala/knitClient/annotations/Widen;")) {
                    kcw.widenFieldAccess(fn.name, (Boolean) an.values.get(3), (Integer) an.values.get(1));
                }
            }
        }

        //Widen Methods
        for (MethodNode mn : this.clazz.methods) {
            if (mn.invisibleAnnotations == null) {
                continue;
            }
            for (AnnotationNode an : mn.invisibleAnnotations) {
                System.out.println(an.desc);
                if (an.desc.equals("Lcom/github/amusingimpala/knitClient/annotations/Widen;")) {
                    kcw.widenMethodAccess(mn.name, mn.desc, (Integer) an.values.get(1), (Boolean) an.values.get(3));
                }
            }
        }

        //Add Interfaces
        for (String interf : this.clazz.interfaces) {
            //TODO: Fix generics and signatures
            kcw.tryAddInterface(interf, Optional.empty());
        }

        //Add Accessors     TODO
        //Add Invokers
        //Add Enum Entries

        //Transfer new fields
        for (FieldNode fn : this.clazz.fields) {
            boolean isShadow = false;
            String compatPrefix = "";
            if (fn.invisibleAnnotations != null) {
                for (AnnotationNode an : fn.invisibleAnnotations) {
                    System.out.println(an.desc);
                    if (an.desc.equals("Lcom/github/amusingimpala/knitClient/annotations/Shadow;")) {
                        isShadow = true;
                    }

                }
            }
            for (AnnotationNode an : this.clazz.invisibleAnnotations) {
                System.out.println(an.desc);
                if (an.desc.equals("Lcom/github/amusingimpala/knitClient/annotations/Target;")) {
                    compatPrefix = (String) an.values.get(3);
                }
            }
            if (!isShadow) {
                kcw.addField(this.clazz.name, fn.name, compatPrefix);
            }
        }
        //Transfer new methods
        for (MethodNode mn : this.clazz.methods) {
            boolean isShadow = false;
            String compatPrefix = "";
            if (mn.invisibleAnnotations != null) {
                for (AnnotationNode an : mn.invisibleAnnotations) {
                    System.out.println(an.desc);
                    if (an.desc.equals("Lcom/github/amusingimpala/knitClient/annotations/Shadow;")) {
                        isShadow = true;
                    }

                }
            }
            for (AnnotationNode an : this.clazz.invisibleAnnotations) {
                System.out.println(an.desc);
                if (an.desc.equals("Lcom/github/amusingimpala/knitClient/annotations/Target;")) {
                    compatPrefix = (String) an.values.get(3);
                }
            }
            if (!isShadow && !mn.name.equals("<init>") && !mn.name.equals("<clinit>")) {
                kcw.addMethod(this.clazz.name, mn.name, mn.desc, compatPrefix);
            }
        }

        return this;
    }

    public KnitClassTransformer moveClass(String newClazz) {
        this.clazz = KnitClassWriterV2.getNode(newClazz);
        return this;
    }

    public byte[] write() {
        return this.kcw.write();
    }

    /**
     * Must be called to begin a class transformation
     *
     * @param clazz The class from which to get target class
     */
    public KnitClassTransformer init(String clazz) {
        this.clazz = KnitClassWriterV2.getNode(clazz);
        String targetClass = null;
        for (AnnotationNode an : this.clazz.invisibleAnnotations) {
            System.out.println(an.desc);
            if (an.desc.equals("Lcom/github/amusingimpala/knitClient/annotations/Target;")) {
                targetClass = (String) an.values.get(1);
            }
        }
        kcw = new KnitClassWriterV2(targetClass);
        return this;
    }
}
