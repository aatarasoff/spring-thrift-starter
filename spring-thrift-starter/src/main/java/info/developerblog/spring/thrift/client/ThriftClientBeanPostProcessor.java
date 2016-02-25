package info.developerblog.spring.thrift.client;

import info.developerblog.spring.thrift.annotation.ThriftClient;
import info.developerblog.spring.thrift.client.pool.ThriftClientKey;
import info.developerblog.spring.thrift.client.pool.ThriftClientPool;
import info.developerblog.spring.thrift.client.pool.ThriftClientPooledObjectFactory;
import lombok.SneakyThrows;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import ru.trylogic.spring.boot.thrift.beans.RequestIdLogger;

import java.lang.reflect.Field;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by aleksandr on 01.09.15.
 */
@Component
@Configuration
@ConditionalOnClass(ThriftClient.class)
@ConditionalOnWebApplication
public class ThriftClientBeanPostProcessor implements org.springframework.beans.factory.config.BeanPostProcessor {
    Map<String, Class> beansToProcess = new HashMap<>();

    @Autowired
    TProtocolFactory protocolFactory;

    @Autowired
    LoadBalancerClient loadBalancerClient;

    @Autowired
    PropertyResolver propertyResolver;

    @Autowired
    RequestIdLogger requestIdLogger;

    @Autowired
    DefaultListableBeanFactory beanFactory;

    @Autowired
    KeyedPooledObjectFactory<ThriftClientKey, TServiceClient> thriftClientPoolFactory;

    @Autowired
    KeyedObjectPool<ThriftClientKey, TServiceClient> thriftClientsPool;

    public ThriftClientBeanPostProcessor() {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = bean.getClass();
        do {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(ThriftClient.class)) {
                    beansToProcess.put(beanName, clazz);
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
            for (Field field : beansToProcess.get(beanName).getDeclaredFields()) {
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

    @Bean
    public KeyedObjectPool<ThriftClientKey, TServiceClient> thriftClientsPool() {
        GenericKeyedObjectPoolConfig poolConfig = new GenericKeyedObjectPoolConfig();
        poolConfig.setJmxEnabled(false); //cause spring will autodetect itself
        return new ThriftClientPool(thriftClientPoolFactory, poolConfig);
    }

    @Bean
    public KeyedPooledObjectFactory thriftClientPoolFactory() {
        return ThriftClientPooledObjectFactory
                .builder()
                    .protocolFactory(protocolFactory)
                    .propertyResolver(propertyResolver)
                    .loadBalancerClient(loadBalancerClient)
                    .requestIdLogger(requestIdLogger)
                .build();
    }
}
