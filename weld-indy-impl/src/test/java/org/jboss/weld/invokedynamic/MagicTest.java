package org.jboss.weld.invokedynamic;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.weld.test.FirstBean;
import org.jboss.weld.test.OnPurposeException;
import org.jboss.weld.test.SecondBean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileNotFoundException;

/**
 * @author Antoine Sabot-Durand
 */
@RunWith(Arquillian.class)
public class MagicTest {
    

    @Deployment
    public static Archive<?> createTestArchive() throws FileNotFoundException {

        WebArchive ret = ShrinkWrap
                .create(WebArchive.class, "test.war")
                .addClasses(FirstBean.class,
                        SecondBean.class,
                        Bootstraper.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return ret;
    }

    @Test(expected = OnPurposeException.class)
    public void testFirstBeanCall() {
        SecondBean secondBean=new SecondBean();
        secondBean.doSservice();
        Assert.assertTrue(true);

    }
}
