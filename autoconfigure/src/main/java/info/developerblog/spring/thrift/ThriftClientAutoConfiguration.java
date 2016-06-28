package info.developerblog.spring.thrift;

import info.developerblog.spring.thrift.sleuth.ThriftHttpTransportSpanInjector;
import org.apache.thrift.transport.TTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.sleuth.SpanInjector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by aleksandr on 27.06.16.
 */
@Configuration
public class ThriftClientAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(name = "thriftTransportSpanInjector")
    SpanInjector<TTransport> thriftTransportSpanInjector() {
        return new ThriftHttpTransportSpanInjector();
    }
}
