package info.developerblog.examples.thirft.simpleclient;

import example.TGreetingService;
import example.TName;
import example.TStatus;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertEquals;

/**
 * Created by aleksandr on 01.09.15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")
public class TGreetingServiceHandlerTests {

    @Value("${local.server.port}")
    int port;

    @Autowired
    TProtocolFactory protocolFactory;

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

        assertEquals("Hello John Smith", client.greet(name));

        name.setStatus(TStatus.MR);

        assertEquals("Hello Mr John Smith", client.greet(name));
    }
}
