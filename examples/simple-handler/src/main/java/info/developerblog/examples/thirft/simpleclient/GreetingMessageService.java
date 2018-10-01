package info.developerblog.examples.thirft.simpleclient;

import example.TName;
import org.springframework.stereotype.Service;

@Service
public class GreetingMessageService {

    public String constructGreeting(TName name) {
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
