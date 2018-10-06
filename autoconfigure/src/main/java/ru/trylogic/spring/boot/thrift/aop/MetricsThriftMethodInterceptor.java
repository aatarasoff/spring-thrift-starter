package ru.trylogic.spring.boot.thrift.aop;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class MetricsThriftMethodInterceptor implements MethodInterceptor {
    private static final String THRIFT_REQUEST_DURATION_METRIC = "thrift.request.duration";

    private final MeterRegistry meterRegistry;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final long startTime = System.nanoTime();

        try {
            final Object result = invocation.proceed();
            time(invocation, startTime);
            return result;
        } catch (Exception e) {
            time(invocation, startTime, e);
            throw e;
        } finally {
            meterRegistry.gauge("timer.thrift." + invocation.getThis().getClass().getCanonicalName() + "." + invocation.getMethod().getName(), System.currentTimeMillis() - startTime);
        }
    }

    private void time(MethodInvocation invocation, long startTime) {
        time(invocation, startTime, null);
    }

    private void time(MethodInvocation invocation, long startTime, Exception exception) {
        Timer
                .builder(THRIFT_REQUEST_DURATION_METRIC)
                .publishPercentileHistogram()
                .description("Thrift handler request duration")
                .tags(
                        "handler", invocation.getThis().getClass().getCanonicalName(),
                        "method", invocation.getMethod().getName(),
                        "status", exception == null ? "ok" : exception.getClass().getCanonicalName()
                )
                .register(meterRegistry)
                .record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    }
}
