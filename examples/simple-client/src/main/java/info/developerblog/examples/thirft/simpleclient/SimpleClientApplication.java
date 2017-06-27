package info.developerblog.examples.thirft.simpleclient;

import example.TGreetingService;
import info.developerblog.spring.thrift.annotation.ThriftClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Created by aleksandr on 01.09.15.
 */
@EnableAsync
@SpringBootApplication
public class SimpleClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleClientApplication.class, args);
    }

    @ThriftClient(serviceId = "greeting-service", path = "/api")
    TGreetingService.Client client;
}
