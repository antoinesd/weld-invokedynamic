package org.jboss.weld.invokedynamic;

import org.jboss.weld.test.FirstBean;

/**
 * @author Antoine Sabot-Durand
 */
public class Instead {

    public static void myDoSomeWork(FirstBean bean) {
        System.out.println("Doing something else");
        bean.doSomeWork();
    }
}
