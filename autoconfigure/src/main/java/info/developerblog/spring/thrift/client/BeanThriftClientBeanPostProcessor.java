package info.developerblog.spring.thrift.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import org.apache.thrift.TServiceClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Created by @driver733
 */
@Component
@Configuration
@ConditionalOnWebApplication
@AutoConfigureAfter(ThriftClientBeanPostProcessorService.class)
public class BeanThriftClientBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

    @Autowired
    private DefaultListableBeanFactory beanFactory;

    @Autowired
    private ThriftClientBeanPostProcessorService service;

    public BeanThriftClientBeanPostProcessor() {
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> clazz, String beanName) throws BeansException {
        do {
            for (Field field : clazz.getDeclaredFields()) {
                if (isThriftClient(field.getType())) {
                    registerThriftClientBean(field.getType());
                }
            }
            for (Constructor<?> constructor : clazz.getConstructors()) {
                for (Parameter param : constructor.getParameters()) {
                    if (isThriftClient(param.getType())) {
                        registerThriftClientBean(param.getType());
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

    private void registerThriftClientBean(Class<?> clazz) {
        String beanName = getBeanName(clazz);
        if (!beanFactory.containsBean(beanName)) {
            beanFactory.registerSingleton(beanName, service.getThriftClientInstance(clazz));
        }
    }

    private static String getBeanName(Class<?> clazz) {
        String className = clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1);
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

}
