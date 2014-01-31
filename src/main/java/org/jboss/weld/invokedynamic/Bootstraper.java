package org.jboss.weld.invokedynamic;

import org.jboss.weld.test.FirstBean;

import javax.enterprise.inject.spi.Bean;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * @author Antoine Sabot-Durand
 */
public class Bootstraper {

   /* private static MethodHandle resolveCallMh = null;

    static {
        {
            MethodHandles.Lookup lu = MethodHandles.lookup();
            try {
                resolveCallMh = lu.findStatic(Bootstraper.class, "resolveCall",
                        MethodType.methodType(Object.class, Bean.class,
                                MethodHandle.class));
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }*/

    private static MethodHandle doSomeWorkMh = null;

    static {
        {
            MethodHandles.Lookup lu = MethodHandles.lookup();
            try {
                doSomeWorkMh = lu.findStatic(Instead.class, "myDoSomeWork",
                        MethodType.methodType(void.class, FirstBean.class));
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


    public static CallSite bootstrapGetBean(MethodHandles.Lookup lookup, String name, MethodType methodType,
                                            Object... params) throws NoSuchMethodException, IllegalAccessException,
            NoSuchFieldException {

        MethodHandle impl = (MethodHandle) params[0];
        String fieldName = (String) params[1];

/*
        BeanManager bm = CDI.current().getBeanManager();

        Class owner=lookup.lookupClass();

        bm.createAnnotatedType(owner);



        Field injField = owner.getField(fieldName);



        Bean<?> bean= bm.getBeans(owner).iterator().next();





        for (InjectionPoint ip : bean.getInjectionPoints()) {
            if(ip.getMember().equals(injField))
                //return new ConstantCallSite(resolveCallMh);
            System.out.println("here");
        }*/


        // AnnotatedElement annotatedElement = Magic.reflect(impl);
        // MethodHandle mh = impl;
/*            for (Advice advice : advices) {
                mh = advice.chain(annotatedElement, mh);
            }*/

        return new ConstantCallSite(doSomeWorkMh.asType(methodType));
        //return new ConstantCallSite(impl);
    }


    public static CallSite bootstrapCallBeanMethod(MethodHandles.Lookup lookup, String name, MethodType methodType,
                                                   Object... params) {

        MethodHandle impl = (MethodHandle) params[0];

        System.out.println("*** InvokeDynamicBootstrap -> Calling method on bean : " + params[1]);

        return new ConstantCallSite(impl);
    }


    public static Object resolveCall(Bean<?> bean, MethodHandle toCall) {

        //ContextBeanInstance beanInstance = new ContextBeanInstance()

        return null;
    }
}
