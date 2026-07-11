package info.developerblog.spring.thrift.client;

import info.developerblog.spring.thrift.client.pool.ThriftClientKey;
import info.developerblog.spring.thrift.client.pool.ThriftClientPool;
import info.developerblog.spring.thrift.client.pool.ThriftClientPooledObjectFactory;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocolFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertyResolver;

import lombok.RequiredArgsConstructor;

/**
 * @author jihor (jihor@ya.ru)
 *         Created on 2016-06-14
 */
@Configuration
@ConditionalOnClass(LoadBalancerClient.class)
@RequiredArgsConstructor
public class PoolConfiguration {

    private final TProtocolFactory protocolFactory;
    private final LoadBalancerClient loadBalancerClient;
    private final PropertyResolver propertyResolver;
    private final Tracer tracer;
    private final Propagator propagator;

    @Value("${thrift.client.max.threads:8}")
    private int maxThreads;

    @Value("${thrift.client.max.idle.threads:8}")
    private int maxIdleThreads;

    @Value("${thrift.client.max.total.threads:8}")
    private int maxTotalThreads;

    @Bean
    @ConditionalOnMissingBean(name = "thriftClientsPool")
    public KeyedObjectPool<ThriftClientKey, TServiceClient> thriftClientsPool() {
        GenericKeyedObjectPoolConfig<TServiceClient> poolConfig = new GenericKeyedObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxTotalThreads);
        poolConfig.setMaxIdlePerKey(maxIdleThreads);
        poolConfig.setMaxTotalPerKey(maxThreads);
        poolConfig.setJmxEnabled(false); //cause spring will autodetect itself
        return new ThriftClientPool(thriftClientPoolFactory(), poolConfig);
    }

    private KeyedPooledObjectFactory<ThriftClientKey, TServiceClient> thriftClientPoolFactory() {
        return ThriftClientPooledObjectFactory
                .builder()
                .protocolFactory(protocolFactory)
                .propertyResolver(propertyResolver)
                .loadBalancerClient(loadBalancerClient)
                .tracer(tracer)
                .propagator(propagator)
                .build();
    }
}
