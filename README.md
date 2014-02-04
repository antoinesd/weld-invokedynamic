weld-invokedynamic
=======

This code is a proof of concept for using InvokeDynamics vs proxy in Weld Framework

What is showed
-------------

This project contains tools to rewrite bytecode in class and replace call to injected field (annotated @Inject) by InvokeDynamic call

Pre-requisites to run this poc
-------------

You'll need a JDK 1.7 (tested with update 45). The code is not running under Java 8 right now due to API change in java.lang
.invoke between java 7 and 8.
You'll also need maven 3.x
Finally you'll need latest snapshot version of `arquillian-weld-ee-embedded-1.1` Arquillian container. As this artifact is not
available in any known repo you'll have to clone it and build it yourself by issuing the following commands :

    git clone git@github.com:arquillian/arquillian-container-weld.git
    cd arquillian-container-weld
    mvn clean install


Compiling the code
----------

As we have to rewrite byte code after compilation in invokedynamic module, it's important that you compile the code from Maven
(and not your IDE). You can do it by using `invokedynamic` maven profile as shown below.

Run the code
----------
The code contains 2 CDI beans `FirstBean` and `SecondBean` both Application Scope (so proxified by Weld) and a test. `SecondBean` inject `FirstBean`
and one of this method calls a `FirstBean` method that triggers an exception on purpose to show the exact stack trace of its call.
The Arquillian test inject `SecondBean` and call triggers the `FirstBean` exception through a call to `SecondBean` method.

To see the first Effect of InvokeDynamic start by launching the code *without* InvokeDynamic instrumentation.
You can this simply by issuing `mvn` command (build and test will be launch by default). You'll see the following in your console :

    -------------------------------------------------------
     T E S T S
    -------------------------------------------------------
    Running org.jboss.weld.invokedynamic.WeldTest
    févr. 04, 2014 5:23:13 PM org.jboss.weld.bootstrap.WeldStartup <clinit>
    INFO: WELD-000900: 2.1.0 (Final)
    Contructing Second Bean
    Service in SecondBean calling firstBean
    Constructing First Bean
    Working in firstBean
    Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 1.492 sec <<< FAILURE!
    testFirstBeanCall(org.jboss.weld.invokedynamic.WeldTest)  Time elapsed: 0.065 sec  <<< ERROR!
    java.lang.RuntimeException: throwing exception on purpose
    	at org.jboss.weld.test.FirstBean.doSomeWork(FirstBean.java:22)
    	at org.jboss.weld.test.FirstBean$Proxy$_$$_WeldClientProxy.doSomeWork(Unknown Source)
    	at org.jboss.weld.test.SecondBean.doSservice(SecondBean.java:19)
    	at org.jboss.weld.test.SecondBean$Proxy$_$$_WeldClientProxy.doSservice(Unknown Source)
    	at org.jboss.weld.invokedynamic.WeldTest.testFirstBeanCall(WeldTest.java:42)
    	...

You can see 2 WeldClientProxy in the stack trace and 5 step from exception to test

Now launch with InvokeDynamic by issuing `mvn -Pinvokedynamic` the result will be :


    -------------------------------------------------------
     T E S T S
    -------------------------------------------------------
    Running org.jboss.weld.invokedynamic.WeldTest
    févr. 04, 2014 5:25:26 PM org.jboss.weld.bootstrap.WeldStartup <clinit>
    INFO: WELD-000900: 2.1.0 (Final)
    *** InvokeDynamicBootstrap -> linking bean resolution for field: secondBean
    $$$ Resolver called via InvokeDynamic -> Resolving injected bean in bean: org.jboss.weld.invokedynamic.WeldTest@77315619 in field: secondBean
    Contructing Second Bean
    *** InvokeDynamicBootstrap -> linking method MethodHandle(SecondBean)void on bean: doSservice
    Service in SecondBean calling firstBean
    *** InvokeDynamicBootstrap -> linking bean resolution for field: bean
    $$$ Resolver called via InvokeDynamic -> Resolving injected bean in bean: org.jboss.weld.test.SecondBean@134caf in field: bean
    Constructing First Bean
    *** InvokeDynamicBootstrap -> linking method MethodHandle(FirstBean)void on bean: doSomeWork
    Working in firstBean
    Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 1.544 sec <<< FAILURE!
    testFirstBeanCall(org.jboss.weld.invokedynamic.WeldTest)  Time elapsed: 0.108 sec  <<< ERROR!
    java.lang.RuntimeException: throwing exception on purpose
    	at org.jboss.weld.test.FirstBean.doSomeWork(FirstBean.java:22)
    	at org.jboss.weld.test.SecondBean.doSservice(SecondBean.java:19)
    	at org.jboss.weld.invokedynamic.WeldTest.testFirstBeanCall(WeldTest.java:42)
    	...


No more WeldClientProxy and only 3 step from exception to test. Also note the runtime linking (line starting with `***`) and the
trace of the method resolving bean (line starting with `$$$`)



Going further
---------

There are a lot of point that need to be enhanced in this firs Proof Of Concept. The main I can see are :

### Better Resolution

This code is quite rudimentary regarding bean resolution. The method `Bootstraper.resolveBean()` should be enhanced by a Weld specialist
to use cache and optimize resolution.

### Better Injection Instrumentation

Instrumentation code only detect simple InjectionPoint by looking for `@Inject` annotation on field. We should add all other use case :

* Constructor and getter parameter injection
* Injection in producer parameter
* Instrumentation of `Instance<>` resolution

among others.

### Create a Java Agent

Right now instrumentation is done by a java executable (class `RewriteByteCode` for convenience, we should off course transform it in a Java Agent
to do instrumentation when class are loaded

### Use InvokeDynamic for AOP

One of the hard point : implement Interceptor spec and Decorator with InvokeDynamic. That will probably be the next step in this POC.
