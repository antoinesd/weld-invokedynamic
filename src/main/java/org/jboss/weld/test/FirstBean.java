package org.jboss.weld.test;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author Antoine Sabot-Durand
 */
@ApplicationScoped
public class FirstBean {



    @PostConstruct
    public void init() {
        System.out.println("Constructing First Bean");
    }

    public void doSomeWork() {
        System.out.println("Working in firstBean");

    }
}
