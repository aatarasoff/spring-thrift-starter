package info.developerblog.examples.thirft.simpleclient;

import example.TGreetingService;
import example.TName;
import info.developerblog.spring.thrift.annotation.ThriftClient;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by aleksandr on 01.09.15.
 */
@SpringBootApplication
@EnableAutoConfiguration
@RestController
public class SimpleClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleClientApplication.class, args);
    }

    @ThriftClient
    TGreetingService.Client client;

    @RequestMapping(value = "/fee")
    public String simpleTestFee() throws TException {
        return client.greet(new TName("John", "Stark"));
    }
}
