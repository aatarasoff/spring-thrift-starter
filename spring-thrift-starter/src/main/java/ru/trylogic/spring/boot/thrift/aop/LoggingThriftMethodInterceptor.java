package ru.trylogic.spring.boot.thrift.aop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;

import java.lang.reflect.Method;

@Slf4j
public class LoggingThriftMethodInterceptor implements MethodBeforeAdvice, AfterReturningAdvice {

	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		log.info("Thrift method {}.{}() is called with args: {}",
				target.getClass().getSimpleName(),
				method.getName(),
				args
		);
	}

	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		log.info("Thrift method {}.{}() returns this: {}",
				target.getClass().getSimpleName(),
				method.getName(),
				returnValue);
	}

}
