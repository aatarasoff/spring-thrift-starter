package info.developerblog.examples.thirft.simpleclient;

import example.TGreetingService;
import example.TName;
import info.developerblog.spring.thrift.annotation.ThriftClient;
import org.apache.thrift.TException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;

@Service
public class AsyncGreetingService {

    @ThriftClient(serviceId = "greeting-service", path = "/api")
    TGreetingService.Client client;

    @Async
    public Future<String> getGreeting(String lastName, String firstName) throws TException {
        return new AsyncResult<>(client.greet(new TName(firstName, lastName)));
    }
}
