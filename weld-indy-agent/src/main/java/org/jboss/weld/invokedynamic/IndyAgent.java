package org.jboss.weld.invokedynamic;

import java.lang.instrument.Instrumentation;

/**
 * @author Antoine Sabot-Durand
 */
public class IndyAgent {

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("**** Starting Invoke Dynamic Agent ****");

        instrumentation.addTransformer(new IndyWeldClassFileTransformer());
    }
}
