package org.jboss.weld.invokedynamic;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This Visitor visits all fields of a class and fills the provided injectedFields structure when it encounter a field
 * with @Inject annotation
 */
class InjectedFieldVisitor extends FieldVisitor {

    private final String name;

    private final String desc;

    Set<String> annotations;

    private Map<String, FieldMetaData> injectedFields;

    public InjectedFieldVisitor(FieldVisitor fv, String name, String desc, Map<String, FieldMetaData> injectedFields) {
        super(Opcodes.ASM5, fv);
        this.name = name;
        this.desc = desc;
        this.injectedFields = injectedFields;
        annotations = new HashSet<>();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String annotationDesc, boolean visible) {

        if (annotationDesc.equals("Ljavax/inject/Inject;")) {
            injectedFields.put(IndyWeldClassFileTransformer.cleanSignature(desc), new FieldMetaData(name, annotations));
        } else {
            annotations.add(IndyWeldClassFileTransformer.cleanSignature(annotationDesc));
        }
        return super.visitAnnotation(annotationDesc, visible);
    }

}
