package info.developerblog.examples.thirft.simpleclient;

import example.TGreetingService;
import example.TName;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TApplicationException;
import ru.trylogic.spring.boot.thrift.annotation.ThriftController;

/**
 * Created by aleksandr on 01.09.15.
 */
@ThriftController("/api")
@RequiredArgsConstructor
public class TGreetingServiceHandler implements TGreetingService.Iface {

    private final GreetingMessageService greetingMessageService;

    @Override
    public String greet(TName name) throws TApplicationException {
        return greetingMessageService.constructGreeting(name);
    }
}
