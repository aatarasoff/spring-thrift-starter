package info.developerblog.spring.thrift.client;

import info.developerblog.spring.thrift.annotation.ThriftClientsMap;
import info.developerblog.spring.thrift.client.pool.ThriftClientKey;
import java.lang.reflect.Field;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
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
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * @author jihor (jihor@ya.ru)
 * Created on 2016-06-14
 */

@Component
@Configuration
@ConditionalOnClass(ThriftClientsMap.class)
@ConditionalOnWebApplication
@AutoConfigureAfter(PoolConfiguration.class)
public class ThriftClientsMapBeanPostProcessor implements BeanPostProcessor {
    private Map<String, Class> beansToProcess = new HashMap<>();

    @Autowired
    private DefaultListableBeanFactory beanFactory;

    @Autowired
    private KeyedObjectPool<ThriftClientKey, TServiceClient> thriftClientsPool;

    public ThriftClientsMapBeanPostProcessor() {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = bean.getClass();
        do {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(ThriftClientsMap.class)) {
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
                ThriftClientsMap annotation = AnnotationUtils.getAnnotation(field, ThriftClientsMap.class);

                if (null != annotation) {
                    HashMap clients = new HashMap();
                    for (Map.Entry<String, ThriftClientKey> entry : ((AbstractThriftClientKeyMapper)beanFactory.getBean(annotation.mapperClass())).getMappings().entrySet()) {
                        ProxyFactory proxyFactory = getProxyFactoryForThriftClient(bean, field, entry.getValue().getClazz());
                        addPoolAdvice(proxyFactory, entry.getValue());

                        proxyFactory.setFrozen(true);
                        proxyFactory.setProxyTargetClass(true);
                        clients.put(entry.getKey(), proxyFactory.getProxy());
                    }
                    ReflectionUtils.makeAccessible(field);
                    ReflectionUtils.setField(field, bean, clients);
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

    private ProxyFactory getProxyFactoryForThriftClient(Object bean, Field field, Class clazz) {
        ProxyFactory proxyFactory = null;
        try {
            proxyFactory = new ProxyFactory(
                    BeanUtils.instantiateClass(
                            clazz.getConstructor(TProtocol.class),
                            (TProtocol) null
                    )
            );
        } catch (NoSuchMethodException e) {
            throw new InvalidPropertyException(bean.getClass(), field.getName(), e.getMessage());
        }
        return proxyFactory;
    }

    @SuppressWarnings("unchecked")
    private void addPoolAdvice(ProxyFactory proxyFactory, ThriftClientKey key) {
        proxyFactory.addAdvice((MethodInterceptor) methodInvocation -> {
            Object[] args = methodInvocation.getArguments();

            TServiceClient thriftClient = null;

            try {
                thriftClient = thriftClientsPool.borrowObject(key);
                return ReflectionUtils.invokeMethod(methodInvocation.getMethod(), thriftClient, args);
            } catch (UndeclaredThrowableException e) {
                if (TException.class.isAssignableFrom(e.getUndeclaredThrowable().getClass()))
                    throw (TException) e.getUndeclaredThrowable();
                throw e;
            } finally {
                if (null != thriftClient)
                    thriftClientsPool.returnObject(key, thriftClient);
            }
        });
    }
}
