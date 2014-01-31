package org.jboss.weld.invokedynamic;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Magic {
    // these fields are duplicated from ASM to avoid an ASM dependency at runtime
    private static final int KIND_GETFIELD = 1;

    //private static final int KIND_GET_STATIC = 2;
    private static final int KIND_PUTFIELD = 3;

    //private static final int KIND_PUTSTATIC = 4;
    private static final int KIND_INVOKEVIRTUAL = 5;

    private static final int KIND_INVOKESTATIC = 6;

    //private static final int KIND_INVOKESPECIAL = 7;
    //private static final int KIND_NEWINVOKESPECIAL = 8;
    private static final int KIND_INVOKEINTERFACE = 9;

    private static final Constructor<?> NEW_METHODHANDLE_INFO;

    private static final Method GET_REFERENCE_KIND;

    private static final Method GET_DECLARING_CLASS;

    private static final Method GET_NAME;

    private static final Method GET_METHOD_TYPE;

    static {
        Class<?> clazz;
        Constructor<?> newMethodHandleInfo;
        Method getReferenceKind, getDeclaringClass, getName, getMethodType;
        try {
            clazz = Class.forName("java.lang.invoke.MethodHandleInfo");
            newMethodHandleInfo = clazz.getDeclaredConstructor(MethodHandle.class);
            getReferenceKind = clazz.getDeclaredMethod("getReferenceKind");
            getDeclaringClass = clazz.getDeclaredMethod("getDeclaringClass");
            getName = clazz.getDeclaredMethod("getName");
            getMethodType = clazz.getDeclaredMethod("getMethodType");
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
            throw new AssertionError(e);
        }
        newMethodHandleInfo.setAccessible(true);
        getReferenceKind.setAccessible(true);
        getDeclaringClass.setAccessible(true);
        getName.setAccessible(true);
        getMethodType.setAccessible(true);
        NEW_METHODHANDLE_INFO = newMethodHandleInfo;
        GET_REFERENCE_KIND = getReferenceKind;
        GET_DECLARING_CLASS = getDeclaringClass;
        GET_NAME = getName;
        GET_METHOD_TYPE = getMethodType;
    }

    public static AnnotatedElement reflect(MethodHandle mh) {
        Object methodHandleInfo;
        int referenceKind;
        Class<?> declaringClass;
        String name;
        MethodType methodType;
        try {
            methodHandleInfo = NEW_METHODHANDLE_INFO.newInstance(mh);
            referenceKind = (Integer) GET_REFERENCE_KIND.invoke(methodHandleInfo);
            declaringClass = (Class<?>) GET_DECLARING_CLASS.invoke(methodHandleInfo);
            name = (String) GET_NAME.invoke(methodHandleInfo);
            methodType = (MethodType) GET_METHOD_TYPE.invoke(methodHandleInfo);
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            throw new AssertionError(e);
        }

        return reflect(referenceKind, declaringClass, name, methodType);
    }

    private static AnnotatedElement reflect(int referenceKind, Class<?> declaringClass, String name, MethodType methodType) {
        try {
            switch (referenceKind) {
                case KIND_GETFIELD:
                case KIND_PUTFIELD:
                    return declaringClass.getDeclaredField(name);

                case KIND_INVOKEVIRTUAL:
                case KIND_INVOKEINTERFACE:
                case KIND_INVOKESTATIC:
                    return declaringClass.getDeclaredMethod(name, methodType.parameterArray());

                default:
                    throw new AssertionError("bad kind " + referenceKind);
            }
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }
  
  /*
  public interface F0 extends Serializable { public Object appply(); }
  public interface F1<T0> extends Serializable { public Object appply(T0 arg0); }
  public interface F2<T0,T1> extends Serializable { public Object appply(T0 arg0, T1 arg1); }
  public interface V0 extends Serializable { public void appply(); }
  public interface V1<T0> extends Serializable { public void appply(T0 arg0); }
  public interface V2<T0,T1> extends Serializable { public void appply(T0 arg0, T1 arg1); }
  
  public static MethodHandle asMHF0(F0 fun) { return extractMH(fun); }
  public static <T0> MethodHandle asMHF1(F1<T0> fun) { return extractMH(fun); }
  public static <T0,T1> MethodHandle asMHF2(F2<T0,T1> fun) { return extractMH(fun); }
  public static MethodHandle asMHV0(V0 fun) { return extractMH(fun); }
  public static <T0> MethodHandle asMHV1(V1<T0> fun) { return extractMH(fun); }
  public static <T0,T1> MethodHandle asMHV2(V2<T0,T1> fun) { return extractMH(fun); }
  
  private static MethodHandle extractMH(Object fun) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(fun);
      byte[] array = baos.toByteArray();
      class RogueObjectInputStream extends ObjectInputStream {
        ArrayList<Object> serializedValues = new ArrayList<>();
        
        public RogueObjectInputStream(InputStream input) throws IOException {
          super(input);
          enableResolveObject(true);
        }
        
        @Override
        protected Object resolveObject(Object obj) throws IOException {
          serializedValues.add(obj);
          return obj;
        }
      }
      ArrayList<Object> serializedValues;
      try(RogueObjectInputStream rois = new RogueObjectInputStream(new ByteArrayInputStream(array))) {
        rois.readObject();
        serializedValues = rois.serializedValues;
      }
      System.out.println(serializedValues);
      
      String implClass = (String)serializedValues.get(7);
      Integer implKind = (Integer)serializedValues.get(8);
      
      Class<?> declaringClass = Class.forName(implClass);
      
      AnnotatedElement annotatedElement = reflect(implKind,
          declaringClass,
          serializedLambda.getImplMethodName(),
          MethodType.fromMethodDescriptorString(serializedLambda.getImplMethodSignature(), declaringClass.getClassLoader()));
      Method method = (Method)annotatedElement;
      method.setAccessible(true);
      return MethodHandles.publicLookup().unreflect(method);
    } catch(IOException | ClassNotFoundException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }*/
}
