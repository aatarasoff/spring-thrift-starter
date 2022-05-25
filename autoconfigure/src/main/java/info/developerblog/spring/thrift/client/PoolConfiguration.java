package info.developerblog.spring.thrift.client;

import info.developerblog.spring.thrift.client.pool.ThriftClientKey;
import info.developerblog.spring.thrift.client.pool.ThriftClientPool;
import info.developerblog.spring.thrift.client.pool.ThriftClientPooledObjectFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocolFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertyResolver;

/**
 * @author jihor (jihor@ya.ru)
 *         Created on 2016-06-14
 */
@Configuration
@ConditionalOnBean(Tracer.class)
public class PoolConfiguration {

    @Autowired
    private TProtocolFactory protocolFactory;

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private PropertyResolver propertyResolver;

    @Value("${thrift.client.max.threads:8}")
    private int maxThreads;

    @Value("${thrift.client.max.idle.threads:8}")
    private int maxIdleThreads;

    @Value("${thrift.client.max.total.threads:8}")
    private int maxTotalThreads;

    @Autowired
    private Tracer tracer;

    @Bean
    @ConditionalOnMissingBean(name = "thriftClientsPool")
    public KeyedObjectPool<ThriftClientKey, TServiceClient> thriftClientsPool() {
        GenericKeyedObjectPoolConfig poolConfig = new GenericKeyedObjectPoolConfig();
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
                .build();
    }

}
