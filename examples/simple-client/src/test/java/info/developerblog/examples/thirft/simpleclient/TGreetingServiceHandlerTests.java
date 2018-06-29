package info.developerblog.examples.thirft.simpleclient;

import info.developerblog.examples.thirft.simpleclient.configuration.CountingAspect;
import info.developerblog.examples.thirft.simpleclient.configuration.TestAspectConfiguration;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.thrift.transport.TTransportException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import static info.developerblog.examples.thirft.simpleclient.TGreetingServiceController.*;
import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Created by aleksandr on 01.09.15.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    SimpleClientApplication.class,
    TestAspectConfiguration.class
},
    webEnvironment = RANDOM_PORT)
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

    @Value("${thrift.client.max.idle.threads:8}")
    private int maxIdleThreads;

    @Value("${thrift.client.max.total.threads:8}")
    private int maxTotalThreads;

    @Test
    public void testSimpleCall() throws Exception {
        assertEquals("Hello John Smith", greetingService.getGreeting("Smith", "John"));
    }

    @Test(expected = TTransportException.class)
    public void testCallWithTimeout() throws Exception {
        greetingService.getGreetingWithTimeout(TIMEOUTEMULATOR, "John");
    }

    @Test
    public void testMappedClient() throws Exception {
        greetingService.getGreetingForKey("key1", "Doe", "John");
    }

    @Test(expected = TTransportException.class)
    public void testMappedClientWithTimeout() throws Exception {
        greetingService.getGreetingForKey("key2", TIMEOUTEMULATOR, "Jane");
    }

    @Test(expected = TTransportException.class)
    public void testMisconfigurableClient() throws Exception {
        greetingService.getGreetingWithMisconfguration("Doe", "John");
    }

    @Test
    public void testClientWithDefaultRetries() throws Exception {
        countingAspect.counter.set(0);
        try {
            greetingService.getOneOffGreetingWithTimeout(TIMEOUTEMULATOR, "John");
            Assert.fail("TTransportException Expected");
        } catch (TTransportException e){
            Assert.assertEquals(1, countingAspect.counter.intValue());
        }
    }

    @Test
    public void testClientWithMultipleRetries() throws Exception {
        countingAspect.counter.set(0);
        try {
            greetingService.getRetriableGreetingWithTimeout(TIMEOUTEMULATOR, "John");
            Assert.fail("TTransportException Expected");
        } catch (TTransportException e){
            Assert.assertEquals(3, countingAspect.counter.intValue());
        }
    }

    @Test
    public void testClientThreadCount() {
        assertEquals(clientPool.getMaxIdlePerKey(), maxIdleThreads);
        assertEquals(clientPool.getMaxTotalPerKey(), maxThreads);
        assertEquals(clientPool.getMaxTotal(), maxTotalThreads);
    }

}
