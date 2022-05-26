package info.developerblog.examples.thirft.simpleclient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter({ SimpleDiscoveryClientAutoConfiguration.class })
public class DiscoveryClientAutoConfiguration implements ApplicationListener<WebServerInitializedEvent> {

    @Autowired
    private DiscoveryClient simpleDiscoveryClient;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent webServerInitializedEvent) {
        int port = webServerInitializedEvent.getWebServer().getPort();
        for (String serviceId : simpleDiscoveryClient.getServices()) {
            for (ServiceInstance instance : simpleDiscoveryClient.getInstances(serviceId)) {
                ((DefaultServiceInstance) instance).setPort(port);
            }
        }
    }
}
