package info.developerblog.examples.thirft.proxyclient.jdkproxy;

import example.TGreetingService;
import example.TName;
import info.developerblog.spring.thrift.annotation.ThriftClient;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Service
public class JdkProxyGreetingServiceImpl implements JdkProxyGreetingService {

    @ThriftClient(serviceId = "greeting-service", path = "/api")
    TGreetingService.Client client;

    @Override
    public String getGreeting(String lastName, String firstName) throws TException {
        return client.greet(new TName(firstName, lastName));
    }
}
