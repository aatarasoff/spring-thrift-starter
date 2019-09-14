package ru.trylogic.spring.boot.thrift.aop;

import brave.Span;
import brave.Tracer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class TracingThriftMethodInterceptor implements MethodInterceptor {
    private final Tracer tracer;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final Span span = tracer.nextSpan()
                .name(invocation.getMethod().getName())
                .kind(Span.Kind.SERVER)
                .start();

        try {
            final Object result = invocation.proceed();
            span.finish();
            return result;
        } catch (Exception e) {
            span.error(e);
            throw e;
        }
    }
}
