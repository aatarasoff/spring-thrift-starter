package info.developerblog.examples.thirft.simpleclient;

import org.apache.thrift.transport.TTransportException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.assertEquals;

/**
 * Created by aleksandr on 01.09.15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SimpleClientApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port:8080")
public class TGreetingServiceHandlerTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    GreetingService greetingService;

    MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
    }

    @Test
    public void testSimpleCall() throws Exception {
        assertEquals("Hello John Smith", greetingService.getGreeting("Smith", "John"));

        mockMvc.perform(
                MockMvcRequestBuilders.request(HttpMethod.GET, "/fee")
        ).andReturn();
    }

    @Test(expected = TTransportException.class)
    public void testCallWithTimeout() throws Exception {
        greetingService.getGreetingWithTimeout("Smith", "John");
    }
}
