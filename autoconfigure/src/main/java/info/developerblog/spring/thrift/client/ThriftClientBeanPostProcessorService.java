package info.developerblog.spring.thrift.client;

import info.developerblog.spring.thrift.annotation.ThriftClient;
import info.developerblog.spring.thrift.client.pool.ThriftClientKey;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Consumer;

@Component
@Configuration
@AutoConfigureAfter(PoolConfiguration.class)
public class ThriftClientBeanPostProcessorService {

    @Autowired
    private DefaultListableBeanFactory beanFactory;

    @Autowired
    private KeyedObjectPool<ThriftClientKey, TServiceClient> thriftClientsPool;

    @SuppressWarnings("unchecked")
    private void addPoolAdvice(ProxyFactory proxyFactory) {
        proxyFactory.addAdvice((MethodInterceptor) methodInvocation -> getObject(
                methodInvocation,
                getThriftClientKey(
                        (Class<? extends TServiceClient>) methodInvocation.getMethod().getDeclaringClass()
                )
        ));
    }

    private Object getObject(MethodInvocation methodInvocation, ThriftClientKey key) throws Exception {
        TServiceClient thriftClient = null;
        try {
            thriftClient = thriftClientsPool.borrowObject(key);
            return ReflectionUtils.invokeMethod(methodInvocation.getMethod(), thriftClient, methodInvocation.getArguments());
        } catch (UndeclaredThrowableException e) {
            if (TException.class.isAssignableFrom(e.getUndeclaredThrowable().getClass()))
                throw (TException) e.getUndeclaredThrowable();
            throw e;
        } finally {
            if (null != thriftClient) {
                thriftClientsPool.returnObject(key, thriftClient);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addPoolAdvice(ProxyFactory proxyFactory, ThriftClient annotataion) {
        proxyFactory.addAdvice((MethodInterceptor) methodInvocation -> getObject(
                methodInvocation,
                getThriftClientKey(
                        (Class<? extends TServiceClient>) methodInvocation.getMethod().getDeclaringClass(),
                        annotataion
                )
        ));
    }

    private ThriftClientKey getThriftClientKey(Class<? extends TServiceClient> clazz, ThriftClient annotation) {
        return getThriftClientKeyBuilder(clazz)
                .serviceName(annotation.serviceId())
                .path(annotation.path())
                .build();
    }

    private ThriftClientKey.ThriftClientKeyBuilder getThriftClientKeyBuilder(Class<? extends TServiceClient> clazz) {
        return ThriftClientKey.builder()
                .clazz(clazz);
    }

    private ThriftClientKey getThriftClientKey(Class<? extends TServiceClient> clazz) {
        return getThriftClientKeyBuilder(clazz).build();

    }

    public Object getThriftClientInstance(Class<?> clazz) {
        return getThriftClientInstance(clazz, this::addPoolAdvice);
    }

    public Object getThriftClientInstance(Class<?> clazz, ThriftClient annotation) {
        return getThriftClientInstance(clazz, factory -> addPoolAdvice(factory, annotation));
    }

    private Object getThriftClientInstance(Class<?> clazz, Consumer<ProxyFactory> consumer) {
        if (beanFactory.containsBean(clazz.getSimpleName())) {
            return beanFactory.getBean(clazz);
        } else {
            final Object instance;
            ProxyFactory proxyFactory = getProxyFactoryForThriftClient(clazz);

            consumer.accept(proxyFactory);

            proxyFactory.setFrozen(true);
            proxyFactory.setProxyTargetClass(true);

            instance = proxyFactory.getProxy();
            return instance;
        }
    }

    private ProxyFactory getProxyFactoryForThriftClient(Class<?> clazz) {
        ProxyFactory proxyFactory;
        try {
            proxyFactory = new ProxyFactory(
                    BeanUtils.instantiateClass(
                            clazz.getConstructor(TProtocol.class),
                            (TProtocol) null
                    )
            );
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    String.format(
                            "Failed to init thrift client: %s",
                            e.getMessage()
                    )
            );
        }
        return proxyFactory;
    }

}
