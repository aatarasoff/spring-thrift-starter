package info.developerblog.spring.thrift.client.pool;

import info.developerblog.spring.thrift.transport.TLoadBalancerClient;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.env.PropertyResolver;

/**
 * Created by aleksandr on 14.07.15.
 */
public class ThriftClientPooledObjectFactory extends BaseKeyedPooledObjectFactory<ThriftClientKey, TServiceClient> {
    TProtocolFactory protocolFactory;
    LoadBalancerClient loadBalancerClient;
    PropertyResolver propertyResolver;

    public ThriftClientPooledObjectFactory(TProtocolFactory protocolFactory, LoadBalancerClient loadBalancerClient, PropertyResolver propertyResolver) {
        this.protocolFactory = protocolFactory;
        this.loadBalancerClient = loadBalancerClient;
        this.propertyResolver = propertyResolver;
    }

    @Override
    public TServiceClient create(ThriftClientKey key) throws Exception {
        TProtocol protocol = protocolFactory.getProtocol(
                new TLoadBalancerClient(
                        loadBalancerClient,
                        key.getServiceName(),
                        propertyResolver.getProperty(key.getServiceName() + ".path")
                )
        );

        return BeanUtils.instantiateClass(
                key.getClazz().getConstructor(TProtocol.class),
                (TProtocol) protocol
        );
    }

    @Override
    public PooledObject<TServiceClient> wrap(TServiceClient value) {
        return new DefaultPooledObject<>(value);
    }

    @Override
    public void activateObject(ThriftClientKey key, PooledObject<TServiceClient> p) throws Exception {
        super.activateObject(key, p);

        resetAndClose(p);
    }

    @Override
    public void passivateObject(ThriftClientKey key, PooledObject<TServiceClient> p) throws Exception {
        super.passivateObject(key, p);

        resetAndClose(p);
    }

    private void resetAndClose(PooledObject<TServiceClient> p) {
        p.getObject().getInputProtocol().reset();
        p.getObject().getOutputProtocol().reset();
        p.getObject().getInputProtocol().getTransport().close();
        p.getObject().getOutputProtocol().getTransport().close();
    }
}
