package info.developerblog.spring.thrift.client;

import info.developerblog.spring.thrift.annotation.ThriftClient;
import info.developerblog.spring.thrift.client.pool.ThriftClientKey;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Consumer;
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
    private void addPoolAdvice(ProxyFactory proxyFactory, ThriftClient annotation) {
        proxyFactory.addAdvice((MethodInterceptor) methodInvocation -> getObject(
            methodInvocation,
            getThriftClientKey(
                (Class<? extends TServiceClient>) methodInvocation.getMethod().getDeclaringClass(),
                annotation
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

    /**
     * For field injection through reflection.
     *
     * @see ThriftClientFieldInjectorBeanPostProcessor
     */
    public Object getThriftClientInstanceBy(Field field, ThriftClient annotation) {
        final Object result;
        if (beanFactory.containsBean(field.getName())) {
            result = beanFactory.getBean(field.getName());
        } else {
            result = getThriftClientInstance(field.getType(), annotation);
        }
        return result;
    }

    public void registerThriftClientInstanceBy(Field field) {
        registerThriftClientBean(field.getType());
    }

    public void registerThriftClientInstanceBy(Parameter param) {
        registerThriftClientBean(param.getType());
    }

    /**
     * For constructor and field injection using Spring beans.
     *
     * @see ThriftClientBeanRegistererBeanPostProcessor
     */
    private void registerThriftClientBean(Class<?> clazz) {
        String beanName = thriftClientBeanName(clazz);
        if (!beanFactory.containsBean(beanName)) {
            beanFactory.registerSingleton(beanName, getThriftClientInstance(clazz, this::addPoolAdvice));
            System.out.println(beanName);
        }
    }

    private static String thriftClientBeanName(Class<?> clazz) {
        String className = clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1);
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

    private Object getThriftClientInstance(Class<?> clazz, ThriftClient annotation) {
        return getThriftClientInstance(clazz, factory -> addPoolAdvice(factory, annotation));
    }

    private Object getThriftClientInstance(Class<?> clazz, Consumer<ProxyFactory> consumer) {
        final Object instance;
        ProxyFactory proxyFactory = getProxyFactoryForThriftClient(clazz);

        consumer.accept(proxyFactory);

        proxyFactory.setFrozen(true);
        proxyFactory.setProxyTargetClass(true);

        instance = proxyFactory.getProxy();
        return instance;
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
