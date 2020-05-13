package info.developerblog.examples.thirft.simpleclient;

import example.TName;
import org.apache.thrift.TApplicationException;
import org.springframework.stereotype.Service;

@Service
public class GreetingMessageService {

    public String constructGreeting(TName name) throws TApplicationException {
        if (name.getFirstName().equals("John") && name.getSecondName().equals("Doe"))
            throw new TApplicationException("No John Doe allowed");

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
