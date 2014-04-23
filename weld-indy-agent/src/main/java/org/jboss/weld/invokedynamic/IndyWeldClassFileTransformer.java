package org.jboss.weld.invokedynamic;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Antoine Sabot-Durand
 */
public class IndyWeldClassFileTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain
            protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        final ClassReader classReader = new ClassReader(classfileBuffer);
        final ClassWriter classWriter = new ClassWriter(classReader, 0);
        CdiClassVisitor visitor = new CdiClassVisitor(classWriter, classReader);
        classReader.accept(visitor, 0);

        return classWriter.toByteArray();
    }


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
            CdiClassVisitor visitor = new CdiClassVisitor(classWriter, classReader);
            classReader.accept(visitor, 0);
            if (visitor.isModified()) {
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

    public static String cleanSignature(String in) {
        return in.length() < 2 ? in : in.substring(1, in.length() - 1);
    }

}
