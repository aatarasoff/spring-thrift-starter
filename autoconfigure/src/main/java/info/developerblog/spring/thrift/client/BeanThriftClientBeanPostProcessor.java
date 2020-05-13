package info.developerblog.spring.thrift.client;

import org.apache.thrift.TServiceClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

/**
 * Created by aleksandr on 01.09.15.
 */

@Component
@Configuration
@ConditionalOnWebApplication
@AutoConfigureAfter(ThriftClientBeanPostProcessorService.class)
public class BeanThriftClientBeanPostProcessor implements BeanPostProcessor {

    @Autowired
    private DefaultListableBeanFactory beanFactory;

    @Autowired
    private ThriftClientBeanPostProcessorService service;

    public BeanThriftClientBeanPostProcessor() {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
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
        return bean;
    }

    private static boolean isThriftClient(Class<?> param) {
        return param.getSuperclass() == TServiceClient.class;
    }

    private void registerThriftClientBean(Class<?> clazz) {
        String beanName = getBeanName(clazz);
        if (!beanFactory.containsBean(beanName)) {
            beanFactory.registerSingleton(beanName, service.getThriftClientInstance(clazz));
        }
    }

    private static String getBeanName(Class<?> clazz) {
        String className = clazz.getSimpleName();
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

}
