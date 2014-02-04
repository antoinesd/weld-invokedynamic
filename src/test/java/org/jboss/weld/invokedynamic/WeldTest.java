package org.jboss.weld.invokedynamic;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.weld.test.FirstBean;
import org.jboss.weld.test.SecondBean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.FileNotFoundException;

/**
 * @author Antoine Sabot-Durand
 */
@RunWith(Arquillian.class)
public class WeldTest {

    @Deployment
    public static Archive<?> createTestArchive() throws FileNotFoundException {


        WebArchive ret = ShrinkWrap
                .create(WebArchive.class, "test.war")
                .addClasses(FirstBean.class, SecondBean.class, Bootstraper.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return ret;
    }


    @Inject
    SecondBean secondBean;

    @Test
    public void testFirstBeanCall() {
        secondBean.doSservice();
        Assert.assertTrue(true);

    }
}
