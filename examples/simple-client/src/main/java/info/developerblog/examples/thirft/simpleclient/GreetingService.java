package info.developerblog.examples.thirft.simpleclient;

import example.TGreetingService;
import example.TName;
import info.developerblog.spring.thrift.annotation.ThriftClient;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

/**
 * Created by aleksandr on 08.09.15.
 */
@Service
public class GreetingService {

    @ThriftClient(serviceId = "greeting-service", path = "/api")
    TGreetingService.Client client;

    @ThriftClient(serviceId = "greeting-service-with-timeouts", path = "/api")
    TGreetingService.Client clientWithTimeout;

    public String getGreeting(String lastName, String firstName) throws TException {
        return client.greet(new TName(firstName, lastName));
    }

    public String getGreetingWithTimeout(String lastName, String firstName) throws TException {
        return clientWithTimeout.greet(new TName(firstName, lastName));
    }
}
