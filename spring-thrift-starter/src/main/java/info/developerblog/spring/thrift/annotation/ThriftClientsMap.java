package info.developerblog.spring.thrift.annotation;

import info.developerblog.spring.thrift.client.AbstractThriftClientKeyMapper;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jihor (jihor@ya.ru)
 * Created on 2016-06-14
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ThriftClientsMap {
    Class mapperClass() default AbstractThriftClientKeyMapper.class;
}
