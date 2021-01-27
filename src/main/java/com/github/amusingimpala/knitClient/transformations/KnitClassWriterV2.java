package com.github.amusingimpala.knitClient.transformations;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import static org.objectweb.asm.Opcodes.*;


@SuppressWarnings({"UnusedReturnValue", "unused"})
public class KnitClassWriterV2 {

    private final String clazz;
    private final ClassReader reader;
    private final ClassNode node;

    /**
     * Constructor for a Class transformer
     *
     * @param clazz Name of target class
     *
     */
    public KnitClassWriterV2(String clazz) {
        this.clazz = clazz;
        try {
            this.reader = new ClassReader(clazz);
        } catch (IOException e) {
            e.printStackTrace();
            throw new NullPointerException();
        }
        this.node = new ClassNode();
        this.reader.accept(this.node, 0);
    }

    /**
     * Writes transformed ClassNode into an array of byte for class definition
     *
     * @return Bytes to be passed to FileWriter or ClassLoader
     */
    public byte[] write() {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Adds a given interface to the ClassNode if not already present
     *
     * @param interfaceName Name of the interface to be added
     * @param genericSig An optional entry for the types if it is generic
     */
    @SuppressWarnings("StringConcatenationInLoop")
    public KnitClassWriterV2 tryAddInterface(String interfaceName, Optional<List<String>> genericSig) {
        if (!node.interfaces.contains(interfaceName)) {

            if (genericSig.isPresent()) {
                if (node.signature == null) {
                    //Add super
                    node.signature = "L"+node.superName+";";

                    //Add interfaces
                    for (String interfac3 : node.interfaces) {
                        node.signature = node.signature + "L"+interfac3+";";
                    }

                    node.signature = node.signature + "L"+interfaceName+"<";

                    for (String type : genericSig.get()) {
                        node.signature = node.signature + "L"+type+";";
                    }
                    node.signature = node.signature+">;";
                } else {
                    node.signature = node.signature + "L"+interfaceName+"<";

                    for (String type : genericSig.get()) {
                        node.signature = node.signature + "L"+type+";";
                    }
                    node.signature = node.signature+">;";
                }
            } else if (node.signature != null) {
                node.signature = node.signature+"L"+interfaceName+";";
            }

            //node.signature = (node.signature == null ? "Ljava/lang/Object;" : node.signature) + "L" + interfaceName.replace('.', '/') +";";

            node.interfaces.add(interfaceName);
            System.out.println(node.signature);
        }
        return this;
    }

    /**
     * Widens a field to the given visibility level, can also definalize
     *
     * @param field Name of field to be widened
     * @param definalize Whether or not to definalize field (will be ignored if field is not final)
     * @param resultAccess Visibility of resulting field. Will be math maxed with current visibility to ensure no restricting of access
     */
    public KnitClassWriterV2 widenFieldAccess(String field, boolean definalize, int resultAccess) {
        FieldNode fn = null;
        for (FieldNode node : node.fields) {
            if (node.name.equals(field)) {
                fn = node;
                break;
            }
        }

        if (fn == null) {
            System.out.println("Could not find field "+field+" in class "+node.name);
            throw new NullPointerException();
        }

        int access = 0;

        int currentVisibility = Modifier.isPublic(fn.access) ? ACC_PUBLIC : Modifier.isProtected(fn.access) ? ACC_PROTECTED : Modifier.isPrivate(fn.access) ? ACC_PRIVATE : 0;

        access += getWiderAccess(resultAccess, currentVisibility);
        access += definalize ? 0 : Modifier.isFinal(fn.access) ? ACC_FINAL : 0;
        access += Modifier.isStatic(fn.access) ? ACC_STATIC : 0;

        fn.access = access;

        return this;
    }

    /**
     * Widens a field to the given visibility level, can also definalize
     *
     * @param definalize Whether or not to definalize class (will be ignored if class is not final)
     * @param visibility Visibility of resulting class. Will be math maxed with current visibility to ensure no restricting of access
     */
    public KnitClassWriterV2 widenClassAccess(int visibility, boolean definalize) {
        int access = 0;

        int currentVisibility = Modifier.isPublic(node.access) ? ACC_PUBLIC : Modifier.isProtected(node.access) ? ACC_PROTECTED : Modifier.isPrivate(node.access) ? ACC_PRIVATE : 0;

        access += getWiderAccess(visibility, currentVisibility);
        access += definalize ? 0 : Modifier.isFinal(node.access) ? ACC_FINAL : 0;
        access += Modifier.isStatic(node.access) ? ACC_STATIC : 0;

        node.access = access;

        return this;
    }

    /**
     * Widens a method with a given descriptor, and can definalize it
     *
     * @param definalize Whether or not to remove final modifier. Will be ignored if already not final
     * @param method Name of the method
     * @param desc Descriptor of method to widen
     * @param visibility Resulting visibility of method. Cannot restrict visibility
     */
    public KnitClassWriterV2 widenMethodAccess(String method, String desc, int visibility, boolean definalize) {

        MethodNode mn = null;

        for (MethodNode node : node.methods) {
            if (node.name.equals(method) && node.desc.equals(desc)) {
                mn = node;
                break;
            }
        }

        if (mn == null) {
            System.out.println("Could not find method "+method+" with descriptor "+desc+" in class "+node.name);
            throw new NullPointerException();
        }

        int access = 0;

        int currentVisibility = Modifier.isPublic(mn.access) ? ACC_PUBLIC : Modifier.isProtected(mn.access) ? ACC_PROTECTED : Modifier.isPrivate(mn.access) ? ACC_PRIVATE : 0;

        access += getWiderAccess(visibility, currentVisibility);
        access += definalize ? 0 : Modifier.isFinal(mn.access) ? ACC_FINAL : 0;
        access += Modifier.isStatic(mn.access) ? ACC_STATIC : 0;

        mn.access = access;

        return this;
    }

    /**
     * TODO: Javadoc and fix
     */
    public KnitClassWriterV2 addEnumEntries(String sourceEnum, String desc, String compatPrefix) {
        if (!isEnum(node.access)) {
            throw new IllegalStateException("Class "+clazz+" is not an enumeration!");
        }

        if (!node.interfaces.contains("com/github/amusingimpala/knitClient/transformations/EnumAdditionProvider")) {
            System.out.println(node.interfaces.toString());
            throw new IllegalStateException("Enum "+clazz+" does not implement the EnumAdditionProvider");
        }

        MethodNode clinit = null;

        for (MethodNode node : node.methods) {
            if (node.name.equals("<clinit>")) {
                clinit = node;
                break;
            }
        }

        if (clinit == null) {
            System.out.println("Could not find <clinit> in enum "+node.name);
            throw new NullPointerException();
        }

        int existingEnumEntries = 0;

        for (FieldNode fn : node.fields) {
            if (isEnum(fn.access)) {
                existingEnumEntries++;
            }
        }

        ClassNode enumNode = getNode(sourceEnum);

        int addedEnums = 0;

        InsnList entryInitialization = new InsnList();
        InsnList valuesAdditions = new InsnList();

        for (FieldNode fn : enumNode.fields) {
            if (isEnum(fn.access)) {
                //Add field entries
                String name = compatPrefix+"_"+fn.name;
                Object[] params;
                try {
                    params = ((EnumAdditionProvider)this.getClass().getClassLoader().loadClass(sourceEnum.replace('/', '.')).getField(fn.name).get(null)).getParams();
                } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                    throw new IllegalStateException("Could not acquire params of field "+fn.name+" of enum "+sourceEnum);
                }

                node.fields.add(existingEnumEntries, new FieldNode(ACC_PUBLIC+ACC_STATIC+ACC_FINAL+ACC_ENUM, name, "L"+clazz+";", null, null));

                LabelNode ln = new LabelNode();
                entryInitialization.add(ln);
                entryInitialization.add(new LineNumberNode(existingEnumEntries+addedEnums+4, ln));
                entryInitialization.add(new TypeInsnNode(NEW, "L"+clazz+";"));
                entryInitialization.add(new InsnNode(DUP));
                entryInitialization.add(new LdcInsnNode(fn.name));
                entryInitialization.add(new IntInsnNode(BIPUSH, existingEnumEntries+addedEnums));
                for (Object value : params) {
                    entryInitialization.add(new LdcInsnNode(value));
                }
                entryInitialization.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, clazz, "<init>", "Ljava/lang/String;I"+desc, false));
                entryInitialization.add(new FieldInsnNode(PUTSTATIC, clazz, name, "L" + clazz + ";"));


                //Add to $VALUES
                valuesAdditions.add(new InsnNode(DUP));
                valuesAdditions.add(new IntInsnNode(BIPUSH, existingEnumEntries+addedEnums));
                valuesAdditions.add(new FieldInsnNode(GETSTATIC, clazz, name, "L" + clazz + ";"));
                valuesAdditions.add(new InsnNode(AASTORE));
            }
        }

        AbstractInsnNode aNewArrayValues = null;
        AbstractInsnNode storeArrayValues = null;

        InsnList clinitInstructions = clinit.instructions;

        boolean alreadyRun = false;

        String valuesName = null;
exit:
        for (MethodNode method : node.methods) {
            if (method.name.equals("values")) {
                for (AbstractInsnNode node : method.instructions) {
                    if (node.getType() == AbstractInsnNode.FIELD_INSN) {
                        valuesName = ((FieldInsnNode)node).name;
                        break exit;
                    }
                }
            }
        }

        System.out.println(valuesName);

leave:
        if (!alreadyRun) {
            alreadyRun = true;
            for (ListIterator<AbstractInsnNode> instructions = clinitInstructions.iterator(); instructions.hasNext(); ) {
                AbstractInsnNode byteCall = instructions.next();
                if (byteCall.getOpcode() == PUTSTATIC && byteCall.getType() == AbstractInsnNode.FIELD_INSN && ((FieldInsnNode)byteCall).name.equals(valuesName)) {
                    storeArrayValues = byteCall;
                    for (byteCall = instructions.previous(); instructions.hasPrevious(); instructions.previous()) {
                        if (byteCall.getOpcode() == ANEWARRAY && byteCall.getType() == AbstractInsnNode.TYPE_INSN) {
                            aNewArrayValues = byteCall;
                            break leave;
                        }
                    }
                }
            }
            throw new IllegalStateException("Could not inject enumeration addition into <clinit> of Enum "+clazz+"!");
        }

        clinitInstructions.insertBefore(aNewArrayValues, entryInitialization);
        clinitInstructions.insertBefore(storeArrayValues, valuesAdditions);

        return this;
    }

    /**
     * Copies a method from a source class to the target class given it does not yet exist
     *
     * @param srcClass Name of class from which to transfer
     * @param method Name of method to be transferred
     * @param desc Descriptor of method to be transferred
     * @param compatPrefix Prefix to be added
     */
    public KnitClassWriterV2 addMethod(String srcClass, String method, String desc, String compatPrefix) {

        ClassNode srcNode = getNode(srcClass);

        MethodNode methodNode = null;
        for (MethodNode mn : srcNode.methods) {
            if (mn.name.equals(method) && mn.desc.equals(desc)) {
                methodNode = mn;
                break;
            }
        }

        for (AbstractInsnNode op : methodNode.instructions) {
            //TODO: field diversion from src class to this class
        }

        methodNode.name = compatPrefix+"_"+methodNode.name;

        for (MethodNode mn : this.node.methods) {
            if (mn.name.equals(method) && mn.desc.equals(desc)) {
                throw new IllegalStateException("Identical method found in target class!");
            }
        }

        this.node.methods.add(methodNode);

        return this;
    }

    /**
     * Copies a field from a given class to the target class
     *
     * @param srcClass Class from which to get field
     * @param field Name of field to be transferred
     * @param compatPrefix Prefix to be appended to field
     */
    public KnitClassWriterV2 addField(String srcClass, String field, String compatPrefix) {

        for (FieldNode fn : this.node.fields) {
            if (fn.name.equals(field)) {
                throw new IllegalStateException("Identical field found in target class!");
            }
        }

        ClassNode srcNode = getNode(srcClass);

        FieldNode fieldNode = null;
        for (FieldNode fn : srcNode.fields) {
            if (fn.name.equals(field)) {
                fieldNode = fn;
                break;
            }
        }

        fieldNode.name = compatPrefix+"_"+fieldNode.name;

        //TODO: Perhaps check for static instances of given class

        this.node.fields.add(fieldNode);

        return this;
    }

    /**
     *  Checks whether the provided access is an enumeration
     *
     * @param mod Access to be checked
     *
     * @return Whether or not the access is an enumeration
     */
    private boolean isEnum(int mod) {
        return (mod & ACC_ENUM) != 0;
    }

    /**
     * Compares two access values (must be visibility ONLY) for which one is wider
     *
     * @param access1 First access
     * @param access2 Second access
     *
     * @return Which ever of the two accesses is wider
     */
    private static int getWiderAccess(int access1, int access2) {
        int acc1sub;
        int acc2sub;
        switch (access1) {
            case ACC_PUBLIC: acc1sub = 4; break;
            case ACC_PROTECTED: acc1sub = 3; break;
            case ACC_PRIVATE: acc1sub = 1; break;
            default: acc1sub = 2; break;
        }
        switch (access2) {
            case ACC_PUBLIC: acc2sub = 4; break;
            case ACC_PROTECTED: acc2sub = 3; break;
            case ACC_PRIVATE: acc2sub = 1; break;
            default: acc2sub = 2; break;
        }
        if (acc1sub > acc2sub) {
            return access1;
        } else {
            return access2;
        }
    }

    /**
     * Gets a class node given its name
     *
     * @param srcClass Name of class to be acquired
     *
     * @return ClassNode of provided class name
     */
    public static ClassNode getNode(String srcClass) {
        ClassReader srcClassReader = null;
        try {
            srcClassReader = new ClassReader(srcClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (srcClassReader == null) {
            throw new NullPointerException("Could not load source class "+srcClass);
        }

        ClassNode srcNode = new ClassNode();
        srcClassReader.accept(srcNode, 0);

        return srcNode;
    }
}
