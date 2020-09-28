package info.developerblog.spring.thrift.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import org.apache.thrift.TServiceClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Created by @driver733.
 */
@Component
@Configuration
@ConditionalOnWebApplication
@AutoConfigureAfter(ThriftClientBeanPostProcessorService.class)
public class ThriftClientBeanRegistererBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

    @Autowired
    private ThriftClientBeanPostProcessorService service;

    public ThriftClientBeanRegistererBeanPostProcessor() {
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> clazz, String beanName) throws BeansException {
        do {
            for (Field field : clazz.getDeclaredFields()) {
                if (isThriftClient(field.getType())) {
                    service.registerThriftClientInstanceBy(field);
                }
            }
            for (Constructor<?> constructor : clazz.getConstructors()) {
                for (Parameter param : constructor.getParameters()) {
                    if (isThriftClient(param.getType())) {
                        service.registerThriftClientInstanceBy(param);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return null;
    }

    private static boolean isThriftClient(Class<?> param) {
        Class<?> superclass = param.getSuperclass();
        boolean result = false;
        while (superclass != null) {
            if (superclass == TServiceClient.class) {
                result = true;
                break;
            } else {
                superclass = superclass.getSuperclass();
            }
        }
        return result;
    }

}
