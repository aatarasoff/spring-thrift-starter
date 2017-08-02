package info.developerblog.examples.thirft.poolfactory;

import example.TGreetingService;
import info.developerblog.examples.thirft.simpleclient.SimpleClientApplication;
import info.developerblog.spring.thrift.client.pool.ThriftClientKey;
import info.developerblog.spring.thrift.client.pool.ThriftClientPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.thrift.TServiceClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.annotation.PostConstruct;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;

/**
 * @author Dmitry Zhikharev (jihor@ya.ru)
 *         Created on 13.09.2016
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SimpleClientApplication.class, webEnvironment = RANDOM_PORT)
public class PoolFactoryTests {
    @Autowired
    private ThriftClientPool thriftClientsPool;

    private final ThriftClientKey thriftClientKey = ThriftClientKey.builder()
                                                                   .clazz(TGreetingService.Client.class)
                                                                   .serviceName("greeting-service")
                                                                   .path("/api")
                                                                   .build();

    private KeyedPooledObjectFactory<ThriftClientKey, TServiceClient> factory;
    @PostConstruct
    public void postConstruct(){
        factory = thriftClientsPool.getFactory();
    }

    @Test
    public void poolFactoryFullObjectLifecycleTest() throws Exception {
        PooledObject<TServiceClient> pooledObject = factory.makeObject(thriftClientKey);
        factory.activateObject(thriftClientKey, pooledObject);
        factory.validateObject(thriftClientKey, pooledObject);
        factory.passivateObject(thriftClientKey, pooledObject);
    }

    @Test
    public void poolFactoryNonstandardObjectLifecycleTest() throws Exception {
        // Pooled object may be created and passivated right away
        // E.g. GenericKeyedObjectPool: returnObject() -> reuseCapacity() -> create() and addIdleObject(), which invokes passivate()
        PooledObject<TServiceClient> pooledObject = factory.makeObject(thriftClientKey);
        factory.passivateObject(thriftClientKey, pooledObject);
    }

    @Test
    public void poolFactoryOverlappingObjectLifecyclesTest() throws Exception {
        PooledObject<TServiceClient> pooledObject1 = factory.makeObject(thriftClientKey);

        // activateObject leads to tracer.isEnabled() --> true
        factory.activateObject(thriftClientKey, pooledObject1);
        factory.validateObject(thriftClientKey, pooledObject1);

        // Create another pooled object and passivate it right away.
        // See poolFactoryNonstandardObjectLifecycleTest() for details
        PooledObject<TServiceClient> pooledObject2 = factory.makeObject(thriftClientKey);
        factory.passivateObject(thriftClientKey, pooledObject2);

    }
}
