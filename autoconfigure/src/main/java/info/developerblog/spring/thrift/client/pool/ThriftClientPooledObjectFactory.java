package info.developerblog.spring.thrift.client.pool;

import info.developerblog.spring.thrift.transport.TLoadBalancerClient;
import lombok.Builder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanInjector;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.core.env.PropertyResolver;

/**
 * Created by aleksandr on 14.07.15.
 */
@Builder
public class ThriftClientPooledObjectFactory extends BaseKeyedPooledObjectFactory<ThriftClientKey, TServiceClient> {
    public static final int DEFAULT_CONNECTION_TIMEOUT = 1000;
    public static final int DEFAULT_READ_TIMEOUT = 30000;
    private TProtocolFactory protocolFactory;
    private LoadBalancerClient loadBalancerClient;
    private PropertyResolver propertyResolver;
    private Tracer tracer;
    private SpanInjector<TTransport> spanInjector;

    @Override
    public TServiceClient create(ThriftClientKey key) throws Exception {
        String serviceName = key.getServiceName();

        String endpoint = propertyResolver.getProperty(serviceName + ".endpoint");

        int connectTimeout = propertyResolver.getProperty(serviceName + ".connectTimeout", Integer.class, DEFAULT_CONNECTION_TIMEOUT);
        int readTimeout = propertyResolver.getProperty(serviceName + ".readTimeout", Integer.class, DEFAULT_READ_TIMEOUT);

        TProtocol protocol;

        if (StringUtils.isEmpty(endpoint)) {
            final TLoadBalancerClient loadBalancerClient = new TLoadBalancerClient(
                    this.loadBalancerClient,
                    serviceName,
                    propertyResolver.getProperty(serviceName + ".path", "") + key.getPath()
            );
            loadBalancerClient.setConnectTimeout(connectTimeout);
            loadBalancerClient.setReadTimeout(readTimeout);

            protocol = protocolFactory.getProtocol(loadBalancerClient);
        } else {
            final THttpClient httpClient = new THttpClient(endpoint);
            httpClient.setConnectTimeout(connectTimeout);
            httpClient.setReadTimeout(readTimeout);

            protocol = protocolFactory.getProtocol(httpClient);
        }

        return BeanUtils.instantiateClass(
                key.getClazz().getConstructor(TProtocol.class),
                (TProtocol) protocol
        );
    }

    @Override
    public PooledObject<TServiceClient> wrap(TServiceClient value) {
        return new ThriftClientPooledObject<>(value);
    }

    @Override
    public void activateObject(ThriftClientKey key, PooledObject<TServiceClient> p) throws Exception {
        super.activateObject(key, p);
        ThriftClientPooledObject<TServiceClient> pooledObject = (ThriftClientPooledObject<TServiceClient>) p;

        Span span = this.tracer.createSpan(key.getServiceName());

        span.logEvent(Span.CLIENT_SEND);
        pooledObject.setSpan(span);

        TTransport transport = pooledObject.getObject().getOutputProtocol().getTransport();
        spanInjector.inject(span, transport);
    }

    @Override
    public void passivateObject(ThriftClientKey key, PooledObject<TServiceClient> p) throws Exception {
        ThriftClientPooledObject<TServiceClient> pooledObject = (ThriftClientPooledObject<TServiceClient>) p;
        TTransport transport = pooledObject.getObject().getOutputProtocol().getTransport();

        if (transport instanceof THttpClient) {
            ((THttpClient)transport).setCustomHeaders(null);
        } else {
            ((TLoadBalancerClient)transport).setCustomHeaders(null);
        }

        resetAndClose(p);

        super.passivateObject(key, pooledObject);

        if (this.tracer.isTracing() && pooledObject.getSpan() != null) {
            Span span = pooledObject.getSpan();
            span.logEvent(Span.CLIENT_RECV);
            this.tracer.close(span);
        }
    }

    private void resetAndClose(PooledObject<TServiceClient> p) {
        p.getObject().getInputProtocol().reset();
        p.getObject().getOutputProtocol().reset();
        p.getObject().getInputProtocol().getTransport().close();
        p.getObject().getOutputProtocol().getTransport().close();
    }
}
