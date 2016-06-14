package info.developerblog.spring.thrift.client;

import info.developerblog.spring.thrift.client.pool.ThriftClientKey;
import java.util.Map;

/**
 * @author jihor (jihor@ya.ru)
 *         Created on 2016-06-14
 */
public abstract class AbstractThriftClientKeyMapper {
    abstract public Map<String, ThriftClientKey> getMappings();
}
