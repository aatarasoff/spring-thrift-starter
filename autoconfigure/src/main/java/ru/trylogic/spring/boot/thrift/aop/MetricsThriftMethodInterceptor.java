package ru.trylogic.spring.boot.thrift.aop;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@RequiredArgsConstructor
public class MetricsThriftMethodInterceptor implements MethodInterceptor {

    private final MeterRegistry meterRegistry;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            meterRegistry.gauge("timer.thrift." + invocation.getThis().getClass().getCanonicalName() + "." + invocation.getMethod().getName(), System.currentTimeMillis() - startTime);
        }
    }
}
