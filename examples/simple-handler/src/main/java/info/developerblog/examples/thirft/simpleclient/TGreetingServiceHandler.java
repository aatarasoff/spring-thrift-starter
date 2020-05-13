package info.developerblog.examples.thirft.simpleclient;

import example.TGreetingService;
import example.TName;
import org.apache.thrift.TApplicationException;
import org.springframework.beans.factory.annotation.Autowired;
import ru.trylogic.spring.boot.thrift.annotation.ThriftController;

/**
 * Created by aleksandr on 01.09.15.
 */
@ThriftController("/api")
public class TGreetingServiceHandler implements TGreetingService.Iface {

    private final GreetingMessageService greetingMessageService;

    @Autowired
    public TGreetingServiceHandler(GreetingMessageService greetingMessageService) {
        this.greetingMessageService = greetingMessageService;
    }

    @Override
    public String greet(TName name) throws TApplicationException {
        return greetingMessageService.constructGreeting(name);
    }
}
