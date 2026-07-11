package info.developerblog.spring.thrift.client;

import java.util.Map;

@FunctionalInterface
public interface ThriftClientHeaderCustomizer {
    Map<String, String> headers();
}