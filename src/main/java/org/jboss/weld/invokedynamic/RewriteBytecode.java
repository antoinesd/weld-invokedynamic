package org.jboss.weld.invokedynamic;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Antoine Sabot-Durand
 */
public class RewriteBytecode {

    /**
     * A handle to the InvokeDynamic bootstrapGetBean method needed to initialize bootstrapGetBean calls it points to {@link org.jboss
     * .research.invokedynamic.Container#bootstrapGetBean} method
     */


    private static final Handle BOOTSTRAP_GET_BEAN = new Handle(Opcodes.H_INVOKESTATIC,
            Bootstraper.class.getName().replace('.', '/'),
            "bootstrapGetBean",
            MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class,
                    Object[].class).toMethodDescriptorString());


    private static final Handle BOOTSTRAP_CALL_BEAN_METHOD = new Handle(Opcodes.H_INVOKESTATIC,
                Bootstraper.class.getName().replace('.', '/'),
                "bootstrapCallBeanMethod",
                MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class,
                        Object[].class).toMethodDescriptorString());


    /**
     * Return a Bytecode Tag for handle from an opcode. If the opcode is not in the one tested return 0 as a tag handle
     *
     * @param opcode to return the tag handle for
     * @return the corresponding tag handle
     */


    static int asMethodHandleTag(int opcode) {
        switch (opcode) {
            case Opcodes.GETFIELD:
                return Opcodes.H_GETFIELD;
            case Opcodes.PUTFIELD:
                return Opcodes.H_PUTFIELD;
            case Opcodes.INVOKESTATIC:
                return Opcodes.H_INVOKESTATIC;
            case Opcodes.INVOKEVIRTUAL:
                return Opcodes.H_INVOKEVIRTUAL;
            case Opcodes.INVOKEINTERFACE:
                return Opcodes.H_INVOKEINTERFACE;
            default:
                return 0;  // not a trapped opcode
        }
    }


    // Code weaving
    public static void main(String[] args) throws IOException {
        Path directory = Paths.get(args[0]); // get the directory sent in parameter
        System.out.println("rewrite directory " + directory);


        final Map<ClassReader, Path> reader2Path = new HashMap<>();


        // We visit the directory and create an ASM reader for each class file and keep file and reader associated in
        // reader2path map
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) {
                    ClassReader reader = new ClassReader(Files.newInputStream(file));
                    reader2Path.put(reader, file);
                }
                return FileVisitResult.CONTINUE;
            }
        });


        // We walk thru all the class of the application. We have to check if these class use classes having @Interceptable
        // annotaion to change the static call the these class ofr an InvokeDynamic one
        for (final ClassReader classReader : reader2Path.keySet()) {
            final ClassWriter classWriter = new ClassWriter(classReader, 0);
            classReader.accept(new CdiClassVisitor(classWriter, reader2Path, classReader), 0);
        }
    }

    /**
     * This Visitor visits all fields of a class and fills the provided injectedFields structure when it encounter a field
     * with @Inject annotation
     */
    private static class InjectedFieldVisitor extends FieldVisitor {

        private final String name;

        private final String desc;

        Set<String> annotations;

        private Map<String, FieldMetaData> injectedFields;

        public InjectedFieldVisitor(FieldVisitor fv, String name, String desc,Map<String, FieldMetaData> injectedFields) {
            super(Opcodes.ASM5, fv);
            this.name = name;
            this.desc = desc;
            this.injectedFields=injectedFields;
            annotations = new HashSet<>();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String annotationDesc, boolean visible) {

            if (annotationDesc.equals("Ljavax/inject/Inject;")) {
                injectedFields.put(cleanSignature(desc), new FieldMetaData(name,annotations));
        } else
                annotations.add(cleanSignature(annotationDesc));
            return super.visitAnnotation(annotationDesc, visible);
        }


        @Override
        public void visitEnd() {
            super.visitEnd();
        }

        private String cleanSignature(String in)
        {
            return in.substring(1,in.length()-1);
        }
    }

    /**
     * This visitor visit a class and transforms all calls to injected fields method to InvokeDynamic calls
     */
    private static class CdiClassVisitor extends ClassVisitor {
        private final ClassWriter classWriter;

        private final Map<ClassReader, Path> reader2Path;

        private final ClassReader classReader;

        boolean modified;

        Map<String, FieldMetaData> injectedFields;

        public CdiClassVisitor(ClassWriter classWriter, Map<ClassReader, Path> reader2Path, ClassReader classReader) {
            super(Opcodes.ASM5, classWriter);
            this.classWriter = classWriter;
            this.reader2Path = reader2Path;
            this.classReader = classReader;
            injectedFields = new HashMap<>();
        }

        //We visit each Field to look for @Inject annotation
        @Override
        public FieldVisitor visitField(final int access, final String name, final String desc,
                                       final String signature, final Object value) {
            FieldVisitor fv = super.visitField(access, name, desc, signature, value);
            return new InjectedFieldVisitor(fv, name, desc,injectedFields);
        }

        // We visit each method...
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new MethodVisitor(Opcodes.ASM5, mv) {

                // ... and in each method we visit instruction
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                    int methodHandleTag;
                    // if we call an instruction belonging to a class @Interceptable and if the bytecode operation is
                    // known, we replace the static instruction by invokedynamic.
                    if (injectedFields.containsKey(owner) && (methodHandleTag = asMethodHandleTag(opcode)) != 0) {
                        Handle handle = new Handle(methodHandleTag, owner, name, desc);
                        String indyDesc = '(' + ((owner.charAt(0) == '[') ? owner : 'L' + owner + ';') + desc
                                .substring(1);
                        List<Object> params = new ArrayList<Object>(injectedFields.get(owner).getAnnotations());
                        params.add(0,handle);
                        params.add(1,injectedFields.get(owner).getName());
                        visitInvokeDynamicInsn(name, indyDesc, BOOTSTRAP_CALL_BEAN_METHOD, params.toArray()); // Invokedynamic replacement we send a
                        // pointer to a boostrap method that will be used to initialize the dynamic call the first
                        // time.
                        modified = true;
                        return;
                    }
                    super.visitMethodInsn(opcode, owner, name, desc);
                }

                // We do the same for field instruction
               /* @Override
                public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                    int methodHandleTag;
                    if (injectedFields.containsKey(name) && (methodHandleTag = asMethodHandleTag(opcode)) != 0) {
                        Handle handle = new Handle(methodHandleTag, owner, name, desc);
                        String indyDesc = '(' + ((owner.charAt(0) == '[') ? owner : 'L' + owner + ';') + ')' + desc;
                        visitInvokeDynamicInsn(name, indyDesc, BOOTSTRAP_GET_BEAN, handle);
                        modified = true;
                        return;
                    }
                    super.visitFieldInsn(opcode, owner, name, desc);
                }*/
            };
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (modified) {  // if bytecode was modified we write the modified class file to the disk
                try {
                    Path path = reader2Path.get(classReader);
                    Files.write(path, classWriter.toByteArray());
                    System.out.println("rewrite " + path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
