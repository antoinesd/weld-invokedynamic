package org.jboss.weld.test;


import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import javax.enterprise.inject.Instance;

/**
 * @author Antoine Sabot-Durand
 */
public class WeldTest {

    public static void main(String[] args) {

        Weld weld = new Weld();

        WeldContainer container = weld.initialize();

        Instance<Object> instance = container.instance();

        Instance<SecondBean> secondBeanInstance = instance.select(SecondBean.class);

        SecondBean secondBean = secondBeanInstance.get();
        secondBean.doSservice();
        weld.shutdown();
    }
}
