package info.developerblog.spring.thrift.client;

import lombok.RequiredArgsConstructor;
import org.apache.thrift.TServiceClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

/**
 * Created by @driver733.
 */
@Component
@Configuration
@ConditionalOnWebApplication
@AutoConfigureAfter(ThriftClientBeanPostProcessorService.class)
@RequiredArgsConstructor
public class ThriftClientBeanRegistererBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

    private final ThriftClientBeanPostProcessorService service;

    @Override
    public Object postProcessBeforeInstantiation(Class<?> clazz, String beanName) throws BeansException {
        Class<?> current = clazz;
        do {
            for (Field field : current.getDeclaredFields()) {
                if (isThriftClient(field.getType())) {
                    service.registerThriftClientInstanceBy(field);
                }
            }
            for (Constructor<?> constructor : current.getConstructors()) {
                for (Parameter param : constructor.getParameters()) {
                    if (isThriftClient(param.getType())) {
                        service.registerThriftClientInstanceBy(param);
                    }
                }
            }
            current = current.getSuperclass();
        } while (current != null);
        return null;
    }

    private static boolean isThriftClient(Class<?> param) {
        Class<?> superclass = param.getSuperclass();
        while (superclass != null) {
            if (superclass == TServiceClient.class) {
                return true;
            }
            superclass = superclass.getSuperclass();
        }
        return false;
    }
}
