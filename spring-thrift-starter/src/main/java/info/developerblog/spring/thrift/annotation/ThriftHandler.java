package info.developerblog.spring.thrift.annotation;

import org.apache.thrift.protocol.TProtocolFactory;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({java.lang.annotation.ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Component
public @interface ThriftHandler {
    String[] value() default {};
    Class<? extends TProtocolFactory> factory() default TProtocolFactory.class;
}
