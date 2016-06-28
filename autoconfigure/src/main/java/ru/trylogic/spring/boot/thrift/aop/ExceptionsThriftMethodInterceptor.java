package ru.trylogic.spring.boot.thrift.aop;

import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.springframework.aop.ThrowsAdvice;

import java.lang.reflect.Method;

@Slf4j
public class ExceptionsThriftMethodInterceptor implements ThrowsAdvice {
    
    @SuppressWarnings("unused")
    public void afterThrowing(Method method, Object[] args, Object target, Exception e) throws Throwable {
        if (e instanceof TException) {
            throw e;
        }

        log.warn("unexpected exception in " + target.getClass().getCanonicalName() + "." + method.getName(), e);

        throw new TApplicationException(TApplicationException.INTERNAL_ERROR, e.toString());
    }
}
