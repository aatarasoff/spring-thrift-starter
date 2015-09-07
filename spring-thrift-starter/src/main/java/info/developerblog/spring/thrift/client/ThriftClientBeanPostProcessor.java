package info.developerblog.spring.thrift.client;

import info.developerblog.spring.thrift.annotation.ThriftClient;
import info.developerblog.spring.thrift.client.pool.ThriftClientPool;
import info.developerblog.spring.thrift.client.pool.ThriftClientPooledObjectFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * Created by aleksandr on 01.09.15.
 */
@Component
@Configuration
public class ThriftClientBeanPostProcessor implements org.springframework.beans.factory.config.BeanPostProcessor {
    @Autowired
    TProtocolFactory protocolFactory;

    @Autowired
    PropertyResolver propertyResolver;

    @Autowired
    DefaultListableBeanFactory beanFactory;

    @Autowired
    KeyedPooledObjectFactory<Class<? extends TServiceClient>, TServiceClient> thriftClientPoolFactory;

    @Autowired
    KeyedObjectPool<Class<? extends TServiceClient>, TServiceClient> thriftClientsPool;

    public ThriftClientBeanPostProcessor() {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        for (Field field : bean.getClass().getDeclaredFields()) {
            ThriftClient annotation = AnnotationUtils.getAnnotation(field, ThriftClient.class);

            if (null != annotation) {
                if (beanFactory.containsBean(field.getName())) {
                    ReflectionUtils.makeAccessible(field);
                    ReflectionUtils.setField(field, bean, beanFactory.getBean(field.getName()));
                } else {
                    ProxyFactory proxyFactory = getProxyFactoryForThriftClient(bean, field);

                    addPoolAdvice(proxyFactory);

                    proxyFactory.setFrozen(true);
                    proxyFactory.setProxyTargetClass(true);

                    ReflectionUtils.makeAccessible(field);
                    ReflectionUtils.setField(field, bean, proxyFactory.getProxy());
                }
            }
        }
        return bean;
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
    private void addPoolAdvice(ProxyFactory proxyFactory) {
        proxyFactory.addAdvice((MethodInterceptor) methodInvocation -> {
            Object[] args = methodInvocation.getArguments();

            Class<? extends TServiceClient> declaringClass = (Class<? extends TServiceClient>) methodInvocation.getMethod().getDeclaringClass();

            TServiceClient thriftClient = null;
            try {
                thriftClient = thriftClientsPool.borrowObject(declaringClass);
                return ReflectionUtils.invokeMethod(methodInvocation.getMethod(), thriftClient, args);
            } finally {
                if (null != thriftClient)
                    thriftClientsPool.returnObject(declaringClass, thriftClient);
            }
        });
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Bean
    public KeyedObjectPool<Class<? extends TServiceClient>, TServiceClient> thriftClientsPool() {
        GenericKeyedObjectPoolConfig poolConfig = new GenericKeyedObjectPoolConfig();
        poolConfig.setJmxEnabled(false); //cause spring will autodetect itself
        return new ThriftClientPool(thriftClientPoolFactory, poolConfig);
    }

    @Bean
    public KeyedPooledObjectFactory thriftClientPoolFactory() {
        return new ThriftClientPooledObjectFactory(protocolFactory, propertyResolver);
    }
}
