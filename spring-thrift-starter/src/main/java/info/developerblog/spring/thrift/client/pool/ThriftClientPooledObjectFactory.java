package info.developerblog.spring.thrift.client.pool;

import info.developerblog.spring.thrift.transport.TLoadBalancerClient;
import lombok.Builder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.log4j.MDC;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.core.env.PropertyResolver;
import ru.trylogic.spring.boot.thrift.beans.RequestIdLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    private RequestIdLogger requestIdLogger;
    private Tracer tracer;

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
        Map<String, String> headers = sleuthHeaders(span);

        Optional<String> requestId = Optional.ofNullable((String) MDC.get(requestIdLogger.getMDCKey()));
        requestId.ifPresent(reqId -> {
            headers.put(requestIdLogger.getRequestIdHeader(), reqId);
        });

        TTransport transport = pooledObject.getObject().getOutputProtocol().getTransport();
        span.logEvent(Span.CLIENT_SEND);
        pooledObject.setSpan(span);
        if (transport instanceof THttpClient) {
            ((THttpClient)transport).setCustomHeaders(headers);
        } else {
            ((TLoadBalancerClient)transport).setCustomHeaders(headers);
        }
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

        if (this.tracer.isTracing()) {
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

    private Long getParentId(Span span) {
        return !span.getParents().isEmpty() ? span.getParents().get(0) : null;
    }

    private Map<String, String> sleuthHeaders(Span span) {
        Map<String, String> headers = new HashMap<>();
        if (span == null) {
            headers.put(Span.SAMPLED_NAME, Span.SPAN_NOT_SAMPLED);
        } else {
            headers.put(Span.TRACE_ID_NAME, Span.idToHex(span.getTraceId()));
            headers.put(Span.SPAN_NAME_NAME, span.getName());
            headers.put(Span.SPAN_ID_NAME, Span.idToHex(span.getSpanId()));
            headers.put(Span.SAMPLED_NAME, span.isExportable() ?
                    Span.SPAN_SAMPLED : Span.SPAN_NOT_SAMPLED);
            Long parentId = getParentId(span);
            if (parentId != null) {
                headers.put(Span.PARENT_ID_NAME, Span.idToHex(parentId));
            }
            headers.put(Span.PROCESS_ID_NAME, span.getProcessId());
        }
        return headers;
    }
}
