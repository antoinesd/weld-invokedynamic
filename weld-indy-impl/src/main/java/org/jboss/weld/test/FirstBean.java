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
        //Exception below can bun uncommented to compare stack trace with and without invokeDynamic
        throw new RuntimeException("throwing exception on purpose");

    }
}
