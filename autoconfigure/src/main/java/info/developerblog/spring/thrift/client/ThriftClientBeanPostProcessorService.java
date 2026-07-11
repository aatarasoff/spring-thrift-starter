package info.developerblog.spring.thrift.client;

import info.developerblog.spring.thrift.annotation.ThriftClient;
import info.developerblog.spring.thrift.client.pool.ThriftClientKey;
import info.developerblog.spring.thrift.transport.TLoadBalancerClient;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Component
@Configuration
@ConditionalOnClass(LoadBalancerClient.class)
@AutoConfigureAfter(PoolConfiguration.class)
@RequiredArgsConstructor
public class ThriftClientBeanPostProcessorService {

    private final DefaultListableBeanFactory beanFactory;
    private final KeyedObjectPool<ThriftClientKey, TServiceClient> thriftClientsPool;

    @Autowired(required = false)
    private List<ThriftClientHeaderCustomizer> headerCustomizers = Collections.emptyList();

    @SuppressWarnings("unchecked")
    private void addPoolAdvice(ProxyFactory proxyFactory) {
        proxyFactory.addAdvice((MethodInterceptor) methodInvocation -> getObject(
            methodInvocation,
            getThriftClientKey(
                (Class<? extends TServiceClient>) methodInvocation.getMethod().getDeclaringClass()
            )
        ));
    }

    private void applyHeaders(TServiceClient client) {
        if (headerCustomizers.isEmpty()) return;
        TTransport transport = client.getOutputProtocol().getTransport();
        headerCustomizers.stream()
                .flatMap(c -> c.headers().entrySet().stream())
                .forEach(e -> {
                    if (transport instanceof THttpClient t) t.setCustomHeader(e.getKey(), e.getValue());
                    else if (transport instanceof TLoadBalancerClient t) t.setCustomHeader(e.getKey(), e.getValue());
                });
    }

    private Object getObject(MethodInvocation methodInvocation, ThriftClientKey key) throws Exception {
        TServiceClient thriftClient = null;
        try {
            thriftClient = thriftClientsPool.borrowObject(key);
            applyHeaders(thriftClient);
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
        if (beanFactory.containsBean(field.getName())) {
            return beanFactory.getBean(field.getName());
        }
        return getThriftClientInstance(field.getType(), annotation);
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
        ProxyFactory proxyFactory = getProxyFactoryForThriftClient(clazz);

        consumer.accept(proxyFactory);

        proxyFactory.setFrozen(true);
        proxyFactory.setProxyTargetClass(true);

        return proxyFactory.getProxy();
    }

    private ProxyFactory getProxyFactoryForThriftClient(Class<?> clazz) {
        try {
            return new ProxyFactory(
                BeanUtils.instantiateClass(
                    clazz.getConstructor(TProtocol.class),
                    (TProtocol) null
                )
            );
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                String.format("Failed to init thrift client: %s", e.getMessage())
            );
        }
    }
}
