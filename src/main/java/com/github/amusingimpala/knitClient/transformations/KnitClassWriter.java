package com.github.amusingimpala.knitClient.transformations;

import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.objectweb.asm.Opcodes.*;

public class KnitClassWriter {

    private ClassReader reader;
    private final ClassWriter writer;
    private final String clazz;

    public KnitClassWriter(String clazz) {
        this.clazz = clazz;
        try {
            this.reader = new ClassReader(clazz);
            this.writer = new ClassWriter(this.reader, 0);
        } catch (IOException e) {
            throw new NullPointerException("Could not read class "+clazz);
        }
    }

    public KnitClassWriter(byte[] bytes, String clazz) {
        this.reader = new ClassReader(bytes);
        this.writer = new ClassWriter(this.reader, 0);
        this.clazz = clazz;
    }

    public KnitClassWriter widenField(String fieldName) {
        WidenAccess wa = new WidenAccess(fieldName, writer);
        reader.accept(wa, 0);
        return this;
    }

    public KnitClassWriter addEnumValues(String sourceEnum, String desc) {
        EnumAdder ea;
        try {
            ea = new EnumAdder(writer, sourceEnum, desc);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new NullPointerException();
        }
        reader.accept(ea, 0);
        return this;
    }

    public KnitClassWriter addInterface(String interfaceName) {
        InterfaceAdder ia = new InterfaceAdder(writer, interfaceName);
        reader.accept(ia, 0);
        return this;
    }

    public byte[] write() {
        return writer.toByteArray();
    }

    //TODO: Fix static vs non-static fields, do non-public access changes
    public static class WidenAccess extends ClassVisitor {

        private final String field;
        private boolean isPresent;
        private final ClassVisitor visitor;
        private String desc;

        public WidenAccess(String field, ClassVisitor visitor) {
            super(ASM9, visitor);
            this.field = field;
            this.visitor = visitor;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            if (name.equals(field)) {
                isPresent = true;
                desc = descriptor;
                return null;
            }
            return visitor.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public void visitEnd() {
            if (isPresent) {
                FieldVisitor fv = visitor.visitField(Opcodes.ACC_PUBLIC, field, desc, null, null);
                if (fv != null) {
                    fv.visitEnd();
                }
            } else {
                System.out.println("Could not find target method " + field);
            }
            visitor.visitEnd();
        }
    }

    public class EnumAdder extends ClassVisitor {

        private final ClassVisitor visitor;
        private final String desc;
        private final Field[] additions;
        private int enums = 0;
        private boolean addedYet;

        public EnumAdder(ClassVisitor classVisitor, String fromEnum, String desc) throws ClassNotFoundException {
            super(ASM9, classVisitor);
            this.visitor = classVisitor;
            this.additions = this.getClass().getClassLoader().loadClass(fromEnum.replace('/', '.')).getFields();
            this.desc = desc;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (access == ACC_PUBLIC+ACC_STATIC+ACC_FINAL+ACC_ENUM) {
                enums++;
            } else if (!addedYet) {
                addEnums();
                addedYet = true;
            }
            return visitor.visitField(access, name, descriptor, signature, value);
        }

        @Override
        //TODO: Finish abstractifying constructor params
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = visitor.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("<clinit>")) {
                System.out.println("method is <clinit>");
                mv = new ClinitMethodVisitor(mv);
            }
            return mv;
        }

        private void addEnums() {
            FieldVisitor fv;
            for (Field field : this.additions) {
                if (field.isEnumConstant()) {
                    fv = visitor.visitField(ACC_PUBLIC+ACC_STATIC+ACC_FINAL+ACC_ENUM, field.getName(), "L"+clazz+";", null, null);
                    fv.visitEnd();
                }
            }
        }

        @Override
        public void visitEnd() {
            visitor.visitEnd();
        }

        public String getConstructorDesc(String desc) {
            return "(Ljava/lang/String;I"+desc+")V";
        }

        public class ClinitMethodVisitor extends MethodVisitor {

            private final MethodVisitor visitor;

            public ClinitMethodVisitor(MethodVisitor methodVisitor) {
                super(ASM9, methodVisitor);
                this.visitor = methodVisitor;
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                System.out.println("visiting opcode "+opcode);
                if (opcode == ANEWARRAY) {
                    System.out.println("opcode is new array! injecting into enum clinit");
                    int addedEnums = 0;
                    for (Field field : additions) {
                        if (!field.isEnumConstant()) {
                            continue;
                        }
                        Object[] params;
                        try {
                            EnumAdditionProvider enumProvider = (EnumAdditionProvider) field.get(null);
                            params = enumProvider.getParams();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            throw new NullPointerException();
                        }
                        System.out.println("Adding field "+field.getName()+" to enum "+clazz+" clinit");
                        visitor.visitTypeInsn(NEW, clazz);
                        visitor.visitInsn(DUP);
                        visitor.visitLdcInsn(field.getName());
                        visitor.visitIntInsn(BIPUSH, addedEnums+enums);
                        for (Object param : params) {
                            visitor.visitLdcInsn(param);
                        }
                        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz, "<init>", getConstructorDesc(desc), false);
                        visitor.visitFieldInsn(PUTSTATIC, clazz, field.getName(), "L" + clazz + ";");
                        addedEnums++;

                    }
                    visitor.visitIntInsn(BIPUSH, addedEnums+enums);
                }
                visitor.visitTypeInsn(opcode, type);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                if (opcode == PUTSTATIC && name.equals("$VALUES")) {
                    int addedEnums = 0;
                    for (Field field : additions) {
                        visitor.visitInsn(DUP);
                        visitor.visitIntInsn(BIPUSH, addedEnums+enums);
                        visitor.visitFieldInsn(GETSTATIC, clazz, field.getName(), "L" + clazz + ";");
                        visitor.visitInsn(AASTORE);
                        addedEnums++;
                    }
                }
                visitor.visitFieldInsn(opcode, owner, name, descriptor);
            }

            @Override
            public void visitInsn(int opcode) {
                super.visitInsn(opcode);
            }
        }
    }

    public static class InterfaceAdder extends ClassVisitor {

        private final String interfaceName;

        public InterfaceAdder(ClassVisitor classVisitor, String interfaceName) {
            super(ASM9, classVisitor);
            this.interfaceName = interfaceName;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            boolean already = false;
            for (String int3face : interfaces) {
                if (int3face.equals(interfaceName)) {
                    already = true;
                    break;
                }
            }
            if (!already) {
                String[] holding = new String[interfaces.length + 1];
                holding[holding.length - 1] = interfaceName;
                System.arraycopy(interfaces, 0, holding, 0, interfaces.length);
                cv.visit(V1_8, access, name, signature, superName, holding);
            } else {
                cv.visit(V1_8, access, name, signature, superName, interfaces);
            }
        }

    }
}
