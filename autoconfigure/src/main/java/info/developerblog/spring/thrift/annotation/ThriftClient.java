package info.developerblog.spring.thrift.annotation;

import java.lang.annotation.*;

/**
 * Created by aleksandr on 01.09.15.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ThriftClient {
    String serviceId() default "";

    String path() default "";
}
