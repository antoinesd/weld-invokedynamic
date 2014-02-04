package org.jboss.weld.invokedynamic;

import org.jboss.weld.Container;
import org.jboss.weld.bean.proxy.ContextBeanInstance;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.jboss.weld.serialization.spi.ContextualStore;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.jboss.weld.bootstrap.api.helpers.RegistrySingletonProvider.STATIC_INSTANCE;

/**
 * @author Antoine Sabot-Durand
 */
public class Bootstraper {

    private static MethodHandle resolveCallMh = null;

    static {
        {
            MethodHandles.Lookup lu = MethodHandles.lookup();
            try {
                resolveCallMh = lu.findStatic(Bootstraper.class, "resolveBean",
                        MethodType.methodType(Object.class, Object.class, String.class));
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

   /* private static MethodHandle doSomeWorkMh = null;

    static {
        {
            MethodHandles.Lookup lu = MethodHandles.lookup();
            try {
                doSomeWorkMh = lu.findStatic(Bootstraper.class, "myDoSomeWork",
                        MethodType.methodType(void.class, FirstBean.class));
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }*/


    public static CallSite bootstrapGetBean(MethodHandles.Lookup lookup, String name, MethodType methodType,
                                            String fieldName) throws NoSuchMethodException, IllegalAccessException,
            NoSuchFieldException {

        MethodHandle mh = MethodHandles.insertArguments(resolveCallMh, 1, fieldName);
        System.out.println("*** InvokeDynamicBootstrap -> linking bean resolution for field: " + fieldName);

        return new ConstantCallSite(mh.asType(methodType));
    }


    public static CallSite bootstrapCallBeanMethod(MethodHandles.Lookup lookup, String name, MethodType methodType,
                                                   Object... params) {

        MethodHandle impl = (MethodHandle) params[0];

        System.out.println("*** InvokeDynamicBootstrap -> linking method "+ impl.toString() +" on bean: " + name);

        //Stuff about Interceptor or decorator should be done here

        // AnnotatedElement annotatedElement = Magic.reflect(impl);
        // MethodHandle mh = impl;
/*            for (Advice advice : advices) {
                mh = advice.chain(annotatedElement, mh);
            }*/

        //return new ConstantCallSite(doSomeWorkMh);
        return new ConstantCallSite(impl);
    }


    public static Object resolveBean(Object from, String fieldName) throws Throwable {

        BeanManager bm = CDI.current().getBeanManager();

        System.out.println("$$$ Resolver called via InvokeDynamic -> Resolving injected bean in bean: " +from.toString() +" in field: " + fieldName);

        Class owner = from.getClass();
        Bean<?> bean = bm.getBeans(owner).iterator().next();
        InjectionPoint injectionPoint = null;

        for (InjectionPoint ip : bean.getInjectionPoints()) {
            if (ip.getMember().getName().equals(fieldName))
                injectionPoint = ip;
        }

        if (injectionPoint == null)
            throw new IllegalArgumentException("unable to find injection point on field " + fieldName + " in bean class " +
                    owner);

        Bean resBean = bm.getBeans(injectionPoint.getType(), injectionPoint.getQualifiers().toArray(new Annotation[0]))
                .iterator().next();
        BeanIdentifier id = Container.instance(STATIC_INSTANCE).services().get(ContextualStore.class).putIfAbsent(resBean);


        ContextBeanInstance beanInstance = new ContextBeanInstance(resBean, id, STATIC_INSTANCE);

        return beanInstance.getInstance();
    }

   /* public static void myDoSomeWork(FirstBean bean) {
        System.out.println("Doing something else");
        bean.doSomeWork();
    }*/
}
