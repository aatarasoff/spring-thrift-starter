package info.developerblog.examples.thirft.simpleclient;

import example.TGreetingService;
import example.TName;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Created by aleksandr on 01.09.15.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class, TGreetingServiceHandlerTests.MockConfiguration.class}, webEnvironment = RANDOM_PORT)
public class TGreetingServiceHandlerTests {

    @LocalServerPort
    int port;

    @Autowired
    TProtocolFactory protocolFactory;

    @Autowired
    GreetingMessageService greetingMessageService;

    TGreetingService.Iface client;

    @Before
    public void setUp() throws Exception {
        TTransport transport = new THttpClient("http://localhost:" + port + "/api");

        TProtocol protocol = protocolFactory.getProtocol(transport);

        client = new TGreetingService.Client(protocol);
    }

    @Test
    public void testSimpleCall() throws Exception {
        TName name = new TName("John", "Smith");
        doReturn("Hello Mr John Smith").when(greetingMessageService).constructGreeting(name);

        assertEquals("Hello Mr John Smith", client.greet(name));
    }

    @Test(expected = TApplicationException.class)
    public void testThrowException() throws Exception {
        doThrow(new RuntimeException()).when(greetingMessageService).constructGreeting(any());

        client.greet(new TName("John", "Doe"));
    }

    @TestConfiguration
    public static class MockConfiguration {

        @Bean
        @Primary
        public GreetingMessageService greetingMessageService() {
            return mock(GreetingMessageService.class);
        }
    }
}
