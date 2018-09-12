package info.developerblog.spring.thrift.aop;

import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.ThrowsAdvice;

import java.lang.reflect.Method;

@Slf4j
public class LoggingThriftClientMethodInterceptor implements MethodBeforeAdvice, AfterReturningAdvice, ThrowsAdvice {

    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        log.info("ThriftClient method {}.{}() is called with args: {}",
                target.getClass().getSimpleName(),
                method.getName(),
                args);
    }

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
        log.info("ThriftClient method {}.{}() returns this: {}",
                target.getClass().getSimpleName(),
                method.getName(),
                returnValue);
    }

    @SuppressWarnings("unused")
    public void afterThrowing(Method method, Object[] args, Object target, Exception e) throws Throwable {
        if (!(e instanceof TException)) {
            log.warn("Unexpected exception in " + target.getClass().getCanonicalName() + "." + method.getName(), e);
            throw new TApplicationException(TApplicationException.INTERNAL_ERROR, e.toString());
        }
        log.warn("Exception in ThriftClient method {}.{}() when called with args: {}", target.getClass().getSimpleName(), method.getName(), args, e);
    }
}
