package info.developerblog.examples.thirft.simpleclient.configuration;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jihor (jihor@ya.ru)
 *         Created on 2016-09-30
 */
@Aspect
public class CountingAspect {
    public AtomicInteger counter = new AtomicInteger(0);

    @Pointcut("execution(* org.springframework.cloud.client.loadbalancer.LoadBalancerClient.choose(..))")
    private void loadBalancerServerChoice() {}

    @Before("loadBalancerServerChoice()")
    public void before(){
        counter.incrementAndGet();
    }
}
