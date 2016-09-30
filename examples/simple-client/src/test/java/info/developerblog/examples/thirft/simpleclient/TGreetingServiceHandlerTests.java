package info.developerblog.examples.thirft.simpleclient;

import info.developerblog.examples.thirft.simpleclient.configuration.CountingAspect;
import info.developerblog.examples.thirft.simpleclient.configuration.TestConfiguration;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.thrift.transport.TTransportException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.assertEquals;

/**
 * Created by aleksandr on 01.09.15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {SimpleClientApplication.class, TestConfiguration.class})
@WebAppConfiguration
@IntegrationTest("server.port:8080")
@DirtiesContext
public class TGreetingServiceHandlerTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    GreetingService greetingService;

    @Autowired
    GenericKeyedObjectPool clientPool;

    @Autowired
    CountingAspect countingAspect;

    @Value("${thrift.client.max.threads}")
    private int maxThreads;

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
    }

    @Test(expected = TTransportException.class)
    public void testCallWithTimeout() throws Exception {
        greetingService.getGreetingWithTimeout("Smith", "John");
    }

    @Test
    public void testMappedClient() throws Exception {
        greetingService.getGreetingForKey("key1", "Doe", "John");
    }

    @Test(expected = TTransportException.class)
    public void testMappedClientWithTimeout() throws Exception {
        greetingService.getGreetingForKey("key2", "Doe", "Jane");
    }

    @Test(expected = TTransportException.class)
    public void testMisconfigurableClient() throws Exception {
        greetingService.getGreetingWithMisconfguration("Doe", "John");
    }

    @Test
    public void testClientWithDefaultRetries() throws Exception {
        countingAspect.counter.reset();
        try {
            greetingService.getOneOffGreetingWithTimeout("Doe", "John");
            Assert.fail("TTransportException Expected");
        } catch (TTransportException e){
            Assert.assertEquals(1, countingAspect.counter.intValue());
        }
    }

    @Test
    public void testClientWithMultipleRetries() throws Exception {
        countingAspect.counter.reset();
        try {
            greetingService.getRetriableGreetingWithTimeout("Doe", "John");
            Assert.fail("TTransportException Expected");
        } catch (TTransportException e){
            Assert.assertEquals(3, countingAspect.counter.intValue());
        }
    }

    @Test
    public void testClientThreadCount() {
        assertEquals(clientPool.getMaxIdlePerKey(), maxThreads);
        assertEquals(clientPool.getMaxTotalPerKey(), maxThreads);
        assertEquals(clientPool.getMaxTotal(), maxThreads);
    }

}
