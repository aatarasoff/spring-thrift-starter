package info.developerblog.examples.thirft.simpleclient;

import example.TGreetingService;
import example.TName;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Created by aleksandr on 01.09.15.
 */
@SpringBootTest(
        classes = {Application.class},
        webEnvironment = RANDOM_PORT
)
public class TGreetingServiceHandlerTests {
    @LocalServerPort
    int port;

    @Autowired
    TProtocolFactory protocolFactory;

    @Autowired
    GreetingMessageService greetingMessageService;

    TGreetingService.Iface client;

    @BeforeEach
    public void setUp() throws Exception {
        TTransport transport = new THttpClient("http://localhost:" + port + "/api");

        TProtocol protocol = protocolFactory.getProtocol(transport);

        client = new TGreetingService.Client(protocol);
    }

    @Test
    public void testSimpleCall() throws Exception {
        TName name = new TName("John", "Smith");
        assertEquals("Hello John Smith", client.greet(name));
    }

    @Test
    public void testThrowException() throws Exception {
        assertThrows(TApplicationException.class, () -> {
            client.greet(new TName("John", "Doe"));
        });
    }
}
