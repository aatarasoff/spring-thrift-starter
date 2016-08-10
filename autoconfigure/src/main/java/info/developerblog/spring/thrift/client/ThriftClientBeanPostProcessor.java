package info.developerblog.spring.thrift.client;

import info.developerblog.spring.thrift.annotation.ThriftClient;
import info.developerblog.spring.thrift.client.pool.ThriftClientKey;
import java.lang.reflect.Field;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.instrument.hystrix.SleuthHystrixAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * Created by aleksandr on 01.09.15.
 */

@Component
@Configuration
@ConditionalOnClass(ThriftClient.class)
@ConditionalOnWebApplication
@AutoConfigureAfter(PoolConfiguration.class)
public class ThriftClientBeanPostProcessor implements org.springframework.beans.factory.config.BeanPostProcessor {
    private Map<String, List<Class>> beansToProcess = new HashMap<>();

    @Autowired
    private DefaultListableBeanFactory beanFactory;

    @Autowired
    private KeyedObjectPool<ThriftClientKey, TServiceClient> thriftClientsPool;

    public ThriftClientBeanPostProcessor() {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = bean.getClass();
        do {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(ThriftClient.class)) {
                    if (!beansToProcess.containsKey(beanName)) {
                        beansToProcess.put(beanName, new ArrayList<>());
                    }
                    beansToProcess.get(beanName).add(clazz);
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (beansToProcess.containsKey(beanName)) {
            Object target = getTargetBean(bean);
            for (Class clazz : beansToProcess.get(beanName)) {
                for (Field field : clazz.getDeclaredFields()) {
                    ThriftClient annotation = AnnotationUtils.getAnnotation(field, ThriftClient.class);

                    if (null != annotation) {
                        if (beanFactory.containsBean(field.getName())) {
                            ReflectionUtils.makeAccessible(field);
                            ReflectionUtils.setField(field, target, beanFactory.getBean(field.getName()));
                        } else {
                            ProxyFactory proxyFactory = getProxyFactoryForThriftClient(target, field);

                            addPoolAdvice(proxyFactory, annotation);

                            proxyFactory.setFrozen(true);
                            proxyFactory.setProxyTargetClass(true);

                            ReflectionUtils.makeAccessible(field);
                            ReflectionUtils.setField(field, target, proxyFactory.getProxy());
                        }
                    }
                }
            }
        }
        return bean;
    }

    //We have to get a real bean in order to inject a thrift client into the bean instead of its proxy.
    @SneakyThrows
    private Object getTargetBean(Object bean) {
        Object target = bean;
        while (AopUtils.isAopProxy(target)) {
            target = ((Advised)target).getTargetSource().getTarget();
        }
        return target;
    }

    private ProxyFactory getProxyFactoryForThriftClient(Object bean, Field field) {
        ProxyFactory proxyFactory = null;
        try {
            proxyFactory = new ProxyFactory(
                    BeanUtils.instantiateClass(
                            field.getType().getConstructor(TProtocol.class),
                            (TProtocol) null
                    )
            );
        } catch (NoSuchMethodException e) {
            throw new InvalidPropertyException(bean.getClass(), field.getName(), e.getMessage());
        }
        return proxyFactory;
    }

    @SuppressWarnings("unchecked")
    private void addPoolAdvice(ProxyFactory proxyFactory, ThriftClient annotation) {
        proxyFactory.addAdvice((MethodInterceptor) methodInvocation -> {
            Object[] args = methodInvocation.getArguments();

            Class<? extends TServiceClient> declaringClass = (Class<? extends TServiceClient>) methodInvocation.getMethod().getDeclaringClass();

            TServiceClient thriftClient = null;

            ThriftClientKey key = ThriftClientKey.builder()
                    .clazz(declaringClass)
                    .serviceName(annotation.serviceId())
                    .path(annotation.path())
                    .build();

            try {
                thriftClient = thriftClientsPool.borrowObject(key);
                return ReflectionUtils.invokeMethod(methodInvocation.getMethod(), thriftClient, args);
            } catch (UndeclaredThrowableException e) {
                if (TException.class.isAssignableFrom(e.getUndeclaredThrowable().getClass()))
                    throw (TException)e.getUndeclaredThrowable();
                throw e;
            } finally {
                if (null != thriftClient)
                    thriftClientsPool.returnObject(key, thriftClient);
            }
        });
    }

}
