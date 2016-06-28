package info.developerblog.spring.thrift.client;

import info.developerblog.spring.thrift.ThriftClientAutoConfiguration;
import info.developerblog.spring.thrift.client.pool.ThriftClientKey;
import info.developerblog.spring.thrift.client.pool.ThriftClientPool;
import info.developerblog.spring.thrift.client.pool.ThriftClientPooledObjectFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.sleuth.SpanInjector;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertyResolver;

/**
 * @author jihor (jihor@ya.ru)
 *         Created on 2016-06-14
 */
@Configuration
@AutoConfigureAfter({ TraceAutoConfiguration.class, ThriftClientAutoConfiguration.class })
@ConditionalOnBean(Tracer.class)
public class PoolConfiguration {

    @Autowired
    private TProtocolFactory protocolFactory;

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private PropertyResolver propertyResolver;

    @Autowired
    private Tracer tracer;

    @Autowired
    private SpanInjector<TTransport> thriftTransportSpanInjector;

    @Bean
    public KeyedObjectPool<ThriftClientKey, TServiceClient> thriftClientsPool() {
        GenericKeyedObjectPoolConfig poolConfig = new GenericKeyedObjectPoolConfig();
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
                .spanInjector(thriftTransportSpanInjector)
                .build();
    }

}
