package info.developerblog.spring.thrift.client.pool;

import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.TServiceClient;
import org.springframework.cloud.sleuth.Span;

/**
 * Created by ggolda on 20/06/16.
 */

/**
 * Container object for {@link org.springframework.cloud.sleuth.Span} object
 * that connected with current {@link TServiceClient} from pool.
 *
 * @param <T>
 */
public class ThriftClientPooledObject<T extends TServiceClient> extends DefaultPooledObject<T> {
    private Span span;

    /**
     * Create a new instance that wraps the provided object so that the pool can
     * track the state of the pooled object.
     *
     * @param object The object to wrap
     */
    public ThriftClientPooledObject(T object) {
        super(object);
    }

    public ThriftClientPooledObject(T client, Span span) {
        super(client);
        this.span = span;
    }

    public Span getSpan() {
        return span;
    }

    public void setSpan(Span span) {
        this.span = span;
    }

}
