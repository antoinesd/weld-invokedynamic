package org.jboss.weld.test;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Antoine Sabot-Durand
 */

@ApplicationScoped
public class SecondBean {

    @Inject
    FirstBean bean;

    public void doSservice() {
        System.out.println("Service in SecondBean calling firstBean");
        bean.doSomeWork();

    }

    @PostConstruct
    public void init() {
        System.out.println("Contructing Second Bean");
    }

}
