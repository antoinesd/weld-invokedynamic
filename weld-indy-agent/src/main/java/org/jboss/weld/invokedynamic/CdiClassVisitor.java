package org.jboss.weld.invokedynamic;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This visitor visit a class and transforms all calls to injected fields method to InvokeDynamic calls
 */
class CdiClassVisitor extends ClassVisitor {


    private static final String BOOTSTRAPER = "org/jboss/weld/invokedynamic/Bootstraper";

    /**
     * A handle to the InvokeDynamic bootstrapGetBean method needed to initialize bootstrapGetBean calls it points to {@link
     * org.jboss
     * .research.invokedynamic.Container#bootstrapGetBean} method
     */
    private static final Handle BOOTSTRAP_GET_BEAN = new Handle(Opcodes.H_INVOKESTATIC,
            BOOTSTRAPER,
            "bootstrapGetBean",
            MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class,
                    String.class).toMethodDescriptorString()
    );


    private static final Handle BOOTSTRAP_CALL_BEAN_METHOD = new Handle(Opcodes.H_INVOKESTATIC,
            BOOTSTRAPER,
            "bootstrapCallBeanMethod",
            MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class,
                    Object[].class).toMethodDescriptorString()
    );
    private final ClassWriter classWriter;

    public boolean isModified() {
        return modified;
    }

    boolean modified = false;

    Map<String, FieldMetaData> injectedFields;

    public CdiClassVisitor(ClassWriter classWriter) {
        super(Opcodes.ASM5, classWriter);
        this.classWriter = classWriter;
        injectedFields = new HashMap<>();
    }

    //We visit each Field to look for @Inject annotation
    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc,
            final String signature, final Object value) {
        FieldVisitor fv = super.visitField(access, name, desc, signature, value);
        return new InjectedFieldVisitor(fv, name, desc, injectedFields);
    }

    // We visit each method...
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new MethodVisitor(Opcodes.ASM5, mv) {

            // ... and in each method we visit instruction
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                int methodHandleTag;
                // if we call an instruction belonging to a class @Interceptable and if the bytecode operation is
                // known, we replace the static instruction by invokedynamic.
                if (injectedFields.containsKey(owner) && (methodHandleTag = IndyWeldClassFileTransformer.asMethodHandleTag(opcode)) != 0) {
                    Handle handle = new Handle(methodHandleTag, owner, name, desc);
                    String indyDesc = '(' + ((owner.charAt(0) == '[') ? owner : 'L' + owner + ';') + desc
                            .substring(1);
                    List<Object> params = new ArrayList<Object>(injectedFields.get(owner).getAnnotations());
                    params.add(0, handle);
                    params.add(1, injectedFields.get(owner).getName());
                    visitInvokeDynamicInsn(name, indyDesc, BOOTSTRAP_CALL_BEAN_METHOD,
                            params.toArray()); // Invokedynamic replacement we send a
                    // pointer to a boostrap method that will be used to initialize the dynamic call the first
                    // time.
                    modified = true;
                    return;
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }

            // We do the same for field instruction
            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                int methodHandleTag;
                if (injectedFields.containsKey(IndyWeldClassFileTransformer.cleanSignature(desc)) && (methodHandleTag = IndyWeldClassFileTransformer
                        .asMethodHandleTag(opcode)) !=
                        0) {
                    // Handle handle = new Handle(methodHandleTag, owner, name, desc);
                    String indyDesc = '(' + ((owner.charAt(0) == '[') ? owner : 'L' + owner + ';') + ')' + desc;
                    // List<Object> params = new ArrayList<Object>(injectedFields.get(cleanSignature(desc))
                    // .getAnnotations());
                    //params.add(0, injectedFields.get(cleanSignature(desc)).getName());
                    visitInvokeDynamicInsn(name, indyDesc, BOOTSTRAP_GET_BEAN,
                            injectedFields.get(IndyWeldClassFileTransformer.cleanSignature(desc))
                                    .getName()
                    );
                    modified = true;
                    return;
                }
                super.visitFieldInsn(opcode, owner, name, desc);
            }
        };
    }
}
