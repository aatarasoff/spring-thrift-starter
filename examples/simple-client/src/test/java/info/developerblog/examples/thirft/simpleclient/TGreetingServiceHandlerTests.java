package info.developerblog.examples.thirft.simpleclient;

import info.developerblog.examples.thirft.simpleclient.configuration.CountingAspect;
import info.developerblog.examples.thirft.simpleclient.configuration.TestAspectConfiguration;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.thrift.transport.TTransportException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import static info.developerblog.examples.thirft.simpleclient.TGreetingServiceController.TIMEOUTEMULATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Created by aleksandr on 01.09.15.
 */
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

    @Test
    public void testCallWithTimeout() throws Exception {
        assertThrows(TTransportException.class, () -> {
            greetingService.getGreetingWithTimeout(TIMEOUTEMULATOR, "John");
        });
    }

    @Test
    public void testMappedClient() throws Exception {
        greetingService.getGreetingForKey("key1", "Doe", "John");
    }

    @Test
    public void testMappedClientWithTimeout() throws Exception {
        assertThrows(TTransportException.class, () -> {
            greetingService.getGreetingForKey("key2", TIMEOUTEMULATOR, "Jane");
        });
    }

    @Test
    public void testMisconfiguredClient() throws Exception {
        assertThrows(TTransportException.class, () -> {
            greetingService.getGreetingWithMisconfguration("Doe", "John");
        });
    }

    @Test
    public void testClientWithDefaultRetries() throws Exception {
        countingAspect.counter.set(0);
        try {
            greetingService.getOneOffGreetingWithTimeout(TIMEOUTEMULATOR, "John");
            Assertions.fail("TTransportException Expected");
        } catch (TTransportException e){
            assertEquals(1, countingAspect.counter.intValue());
        }
    }

    @Test
    public void testClientWithMultipleRetries() throws Exception {
        countingAspect.counter.set(0);
        try {
            greetingService.getRetriableGreetingWithTimeout(TIMEOUTEMULATOR, "John");
            Assertions.fail("TTransportException Expected");
        } catch (TTransportException e){
            assertEquals(3, countingAspect.counter.intValue());
        }
    }

    @Test
    public void testClientThreadCount() {
        assertEquals(clientPool.getMaxIdlePerKey(), maxIdleThreads);
        assertEquals(clientPool.getMaxTotalPerKey(), maxThreads);
        assertEquals(clientPool.getMaxTotal(), maxTotalThreads);
    }

}
