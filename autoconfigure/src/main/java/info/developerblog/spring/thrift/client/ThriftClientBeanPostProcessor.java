package info.developerblog.spring.thrift.client;

import info.developerblog.spring.thrift.annotation.ThriftClient;
import lombok.SneakyThrows;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by aleksandr on 01.09.15.
 */

@Component
@Configuration
@ConditionalOnClass(ThriftClient.class)
@ConditionalOnWebApplication
@AutoConfigureAfter(ThriftClientBeanPostProcessorService.class)
public class ThriftClientBeanPostProcessor implements BeanPostProcessor {

    private final Map<String, List<Class>> beansToProcess = new HashMap<>();

    @Autowired
    private ThriftClientBeanPostProcessorService service;

    public ThriftClientBeanPostProcessor() {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = bean.getClass();
        do {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(ThriftClient.class)) {
                    if (!beansToProcess.containsKey(beanName)) {
                        beansToProcess.put(beanName, new ArrayList<>());
                    }
                    beansToProcess.get(beanName).add(clazz);
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (beansToProcess.containsKey(beanName)) {
            Object target = getTargetBean(bean);
            for (Class clazz : beansToProcess.get(beanName)) {
                for (Field field : clazz.getDeclaredFields()) {
                    ThriftClient annotation = AnnotationUtils.getAnnotation(field, ThriftClient.class);
                    if (null != annotation) {
                        ReflectionUtils.makeAccessible(field);
                        ReflectionUtils.setField(
                                field,
                                target,
                                service.getThriftClientInstance(field.getType(), annotation)
                        );
                    }
                }
            }
        }
        return bean;
    }

    //We have to get a real bean in order to inject a thrift client into the bean instead of its proxy.
    @SneakyThrows
    private Object getTargetBean(Object bean) {
        Object target = bean;
        while (AopUtils.isAopProxy(target)) {
            target = ((Advised) target).getTargetSource().getTarget();
        }
        return target;
    }

}
