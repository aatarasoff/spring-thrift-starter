package info.developerblog.examples.thirft.proxyclient.async;

import info.developerblog.examples.thirft.simpleclient.AsyncGreetingService;
import info.developerblog.examples.thirft.simpleclient.SimpleClientApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = SimpleClientApplication.class, webEnvironment = RANDOM_PORT)
public class TAsyncGreetingServiceHandlerTests {

    @Autowired
    private AsyncGreetingService asyncGreetingService;

    @Test
    public void testAsyncInjectionFail() throws Exception {
        asyncGreetingService.getGreeting("Smith", "John").get();
    }
}
