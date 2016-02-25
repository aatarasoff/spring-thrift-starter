package info.developerblog.examples.thirft.proxyclient.async;

import info.developerblog.examples.thirft.simpleclient.SimpleClientApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {SimpleClientApplication.class, AsyncTestConfiguration.class})
@WebIntegrationTest("server.port:8080")
@DirtiesContext
public class TAsyncGreetingServiceHandlerTests {

    @Autowired
    private AsyncGreetingService asyncGreetingService;

    @Test
    public void testAsyncInjectionFail() throws Exception {
        asyncGreetingService.getGreeting("Smith", "John").get();
    }
}
