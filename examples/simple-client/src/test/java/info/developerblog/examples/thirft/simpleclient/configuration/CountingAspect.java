package info.developerblog.examples.thirft.simpleclient.configuration;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author jihor (jihor@ya.ru)
 *         Created on 2016-09-30
 */
@Aspect
public class CountingAspect {
    public LongAdder counter = new LongAdder();

    @Pointcut("execution(* org.springframework.cloud.client.loadbalancer.LoadBalancerClient.choose(..))")
    private void loadBalancerServerChoice() {}

    @Before("loadBalancerServerChoice()")
    public void before(){
        counter.increment();
    }
}
