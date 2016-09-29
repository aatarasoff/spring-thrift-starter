package info.developerblog.examples.thirft.simpleclient;

import example.TGreetingService;
import example.TName;
import org.apache.thrift.TException;
import ru.trylogic.spring.boot.thrift.annotation.ThriftController;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by aleksandr on 01.09.15.
 */
@ThriftController("/counting-api")
public class TGreetingServiceCountingController implements TGreetingService.Iface {

    public static final LongAdder counter = new LongAdder();

    @Override
    public String greet(TName name) throws TException {
        counter.increment();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "fake";
    }
}
