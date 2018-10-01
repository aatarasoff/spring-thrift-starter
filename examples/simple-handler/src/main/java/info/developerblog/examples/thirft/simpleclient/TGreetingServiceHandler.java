package info.developerblog.examples.thirft.simpleclient;

import example.TGreetingService;
import example.TName;
import ru.trylogic.spring.boot.thrift.annotation.ThriftController;

/**
 * Created by aleksandr on 01.09.15.
 */
@ThriftController("/api")
public class TGreetingServiceHandler implements TGreetingService.Iface {

    private final GreetingMessageService greetingMessageService;

    public TGreetingServiceHandler(GreetingMessageService greetingMessageService) {
        this.greetingMessageService = greetingMessageService;
    }

    @Override
    public String greet(TName name) {
        return greetingMessageService.constructGreeting(name);
    }
}
