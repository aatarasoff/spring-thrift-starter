package ru.trylogic.spring.boot.thrift.aop;

import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.boot.actuate.metrics.GaugeService;

@RequiredArgsConstructor
public class MetricsThriftMethodInterceptor implements MethodInterceptor {

    final GaugeService gaugeService;
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            gaugeService.submit("timer.thrift." + invocation.getThis().getClass().getCanonicalName() + "." + invocation.getMethod().getName(), System.currentTimeMillis() - startTime);
        }
    }
}
