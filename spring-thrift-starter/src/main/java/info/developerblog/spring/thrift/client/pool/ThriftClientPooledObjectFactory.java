package info.developerblog.spring.thrift.client.pool;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.THttpClient;
import org.springframework.beans.BeanUtils;
import org.springframework.core.env.PropertyResolver;

/**
 * Created by aleksandr on 14.07.15.
 */
public class ThriftClientPooledObjectFactory extends BaseKeyedPooledObjectFactory<Class<? extends TServiceClient>, TServiceClient> {
    TProtocolFactory protocolFactory;
    PropertyResolver propertyResolver;

    public ThriftClientPooledObjectFactory(TProtocolFactory protocolFactory, PropertyResolver propertyResolver) {
        this.protocolFactory = protocolFactory;
        this.propertyResolver = propertyResolver;
    }

    @Override
    public TServiceClient create(Class<? extends TServiceClient> key) throws Exception {
        TProtocol protocol = protocolFactory.getProtocol(new THttpClient(propertyResolver.getRequiredProperty(getEndpointPropertyKey(key))));

        return BeanUtils.instantiateClass(
                key.getConstructor(TProtocol.class),
                (TProtocol) protocol
        );
    }

    @Override
    public PooledObject<TServiceClient> wrap(TServiceClient value) {
        return new DefaultPooledObject<>(value);
    }

    @Override
    public void activateObject(Class<? extends TServiceClient> key, PooledObject<TServiceClient> p) throws Exception {
        super.activateObject(key, p);

        resetAndClose(p);
    }

    @Override
    public void passivateObject(Class<? extends TServiceClient> key, PooledObject<TServiceClient> p) throws Exception {
        super.passivateObject(key, p);

        resetAndClose(p);
    }

    private String getEndpointPropertyKey(Class declaringClass) {
        return WordUtils.uncapitalize(declaringClass.getEnclosingClass().getSimpleName()) + ".endpoint";
    }

    private void resetAndClose(PooledObject<TServiceClient> p) {
        p.getObject().getInputProtocol().reset();
        p.getObject().getOutputProtocol().reset();
        p.getObject().getInputProtocol().getTransport().close();
        p.getObject().getOutputProtocol().getTransport().close();
    }
}
