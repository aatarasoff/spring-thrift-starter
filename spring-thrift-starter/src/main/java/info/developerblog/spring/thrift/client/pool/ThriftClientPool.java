package info.developerblog.spring.thrift.client.pool;

import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.thrift.TServiceClient;

/**
 * Created by aleksandr on 03.08.15.
 */
public class ThriftClientPool extends GenericKeyedObjectPool<Class<? extends TServiceClient>, TServiceClient> {
    public ThriftClientPool(KeyedPooledObjectFactory<Class<? extends TServiceClient>, TServiceClient> factory, GenericKeyedObjectPoolConfig config) {
        super(factory, config);
    }
}
