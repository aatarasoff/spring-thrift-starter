package info.developerblog.examples.thirft.simpleclient;

import example.TGreetingService;
import example.TName;
import org.apache.thrift.TException;
import ru.trylogic.spring.boot.thrift.annotation.ThriftController;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by aleksandr on 01.09.15.
 */
@ThriftController("/api")
public class TGreetingServiceController implements TGreetingService.Iface {

    public static final String TIMEOUTEMULATOR = "timeoutemulator";

    @Override
    public String greet(TName name) throws TException {
        if(name.getSecondName().equals(TIMEOUTEMULATOR)) {
            try {
                SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        StringBuilder result = new StringBuilder();

        result.append("Hello ");

        if(name.isSetStatus()) {
            result.append(org.springframework.util.StringUtils.capitalize(name.getStatus().name().toLowerCase()));
            result.append(" ");
        }

        result.append(name.getFirstName());
        result.append(" ");
        result.append(name.getSecondName());

        return result.toString();
    }
}
