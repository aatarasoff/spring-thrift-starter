package info.developerblog.spring.thrift.client;

import info.developerblog.spring.thrift.transport.TLoadBalancerClient;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Applies all registered {@link ThriftClientHeaderCustomizer} headers to a borrowed
 * {@link TServiceClient} before a Thrift method is invoked.  Used by both
 * {@link ThriftClientBeanPostProcessorService} (regular {@code @ThriftClient} proxies)
 * and {@link ThriftClientsMapBeanPostProcessor} (map-based clients) so the logic lives
 * in exactly one place.
 */
@Component
public class ThriftClientHeaderApplier {

    @Autowired(required = false)
    private List<ThriftClientHeaderCustomizer> customizers = Collections.emptyList();

    public void applyHeaders(TServiceClient client) {
        if (customizers.isEmpty()) {
            return;
        }
        TTransport transport = client.getOutputProtocol().getTransport();
        customizers.stream()
                .flatMap(c -> c.headers().entrySet().stream())
                .forEach(e -> {
                    if (transport instanceof THttpClient t) t.setCustomHeader(e.getKey(), e.getValue());
                    else if (transport instanceof TLoadBalancerClient t) t.setCustomHeader(e.getKey(), e.getValue());
                });
    }
}
