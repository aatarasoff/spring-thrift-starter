package info.developerblog.examples.thirft.simpleclient;

import example.TGreetingService;
import example.TName;
import info.developerblog.spring.thrift.annotation.ThriftClient;
import info.developerblog.spring.thrift.annotation.ThriftClientsMap;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by aleksandr on 08.09.15.
 */
@Service
public class GreetingService {

    @ThriftClient(serviceId = "greeting-service", path = "/api")
    TGreetingService.Client client;

    @ThriftClient(serviceId = "greeting-service-with-timeouts", path = "/api")
    TGreetingService.Client clientWithTimeout;

    @ThriftClient(serviceId = "greeting-service-with-timeouts-oneoff", path = "/counting-api")
    TGreetingService.Client oneoffClientWithTimeout;

    @ThriftClient(serviceId = "greeting-service-with-timeouts-retriable", path = "/counting-api")
    TGreetingService.Client retriableClientWithTimeout;

    @ThriftClientsMap(mapperClass = SampleMapper.class)
    Map<String, TGreetingService.Client> clientsMap;

    @ThriftClient(serviceId = "greeting-service-misconfigurable", path = "/api")
    TGreetingService.Client misconfigurableClient;

    public String getGreeting(String lastName, String firstName) throws TException {
        return client.greet(new TName(firstName, lastName));
    }

    public String getGreetingWithTimeout(String lastName, String firstName) throws TException {
        return clientWithTimeout.greet(new TName(firstName, lastName));
    }

    public String getOneOffGreetingWithTimeout(String lastName, String firstName) throws TException {
        return oneoffClientWithTimeout.greet(new TName(firstName, lastName));
    }

    public String getRetriableGreetingWithTimeout(String lastName, String firstName) throws TException {
        return retriableClientWithTimeout.greet(new TName(firstName, lastName));
    }

    public String getGreetingForKey(String key, String lastName, String firstName) throws TException {
        return clientsMap.get(key).greet(new TName(firstName, lastName));
    }

    public String getGreetingWithMisconfguration(String lastName, String firstName) throws TException {
        return misconfigurableClient.greet(new TName(firstName, lastName));
    }
}
