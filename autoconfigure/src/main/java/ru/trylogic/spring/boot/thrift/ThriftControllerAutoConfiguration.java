package ru.trylogic.spring.boot.thrift;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServlet;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.RegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import ru.trylogic.spring.boot.thrift.annotation.ThriftController;
import ru.trylogic.spring.boot.thrift.aop.LoggingThriftMethodInterceptor;
import ru.trylogic.spring.boot.thrift.aop.MetricsThriftMethodInterceptor;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.lang.reflect.Constructor;

@Configuration
@ConditionalOnClass({ThriftController.class})
@ConditionalOnWebApplication
public class ThriftControllerAutoConfiguration {

    public interface ThriftConfigurer {
        void configureProxyFactory(ProxyFactory proxyFactory);
    }

    @Bean
    @ConditionalOnMissingBean(ThriftConfigurer.class)
    ThriftConfigurer thriftConfigurer() {
        return new DefaultThriftConfigurer();
    }

    @Bean
    @ConditionalOnMissingBean(TProtocolFactory.class)
    TProtocolFactory thriftProtocolFactory() {
        return new TBinaryProtocol.Factory();
    }

    @Bean
    @ConditionalOnMissingBean(LoggingThriftMethodInterceptor.class)
    @ConditionalOnProperty(value = "thrift.controller.logging.enabled", matchIfMissing = true)
    LoggingThriftMethodInterceptor loggingThriftMethodInterceptor() {
        return new LoggingThriftMethodInterceptor();
    }

    public static class DefaultThriftConfigurer implements ThriftConfigurer {
        @Autowired(required = false)
        private MeterRegistry meterRegistry;

        @Autowired(required = false)
        private LoggingThriftMethodInterceptor loggingThriftMethodInterceptor;

        public void configureProxyFactory(ProxyFactory proxyFactory) {
            proxyFactory.setOptimize(true);

            if (meterRegistry != null) {
                proxyFactory.addAdvice(new MetricsThriftMethodInterceptor(meterRegistry));
            }
            if (loggingThriftMethodInterceptor != null) {
                proxyFactory.addAdvice(loggingThriftMethodInterceptor);
            }
        }
    }

    @Configuration
    public static class Registrar extends RegistrationBean implements ApplicationContextAware {

        @Getter
        @Setter
        private ApplicationContext applicationContext;

        @Autowired
        private TProtocolFactory protocolFactory;

        @Autowired
        private ThriftConfigurer thriftConfigurer;

        @Override
        protected String getDescription() {
            return "Thrift services";
        }

        @Override
        @SneakyThrows({NoSuchMethodException.class, ClassNotFoundException.class, InstantiationException.class, IllegalAccessException.class})
        protected void register(String description, ServletContext servletContext) {
            for (String beanName : applicationContext.getBeanNamesForAnnotation(ThriftController.class)) {
                ThriftController annotation = applicationContext.findAnnotationOnBean(beanName, ThriftController.class);

                register(servletContext, annotation.value(), protocolFactory.getClass(), applicationContext.getBean(beanName));
            }
        }

        protected void register(ServletContext servletContext, String[] urls, Class<? extends TProtocolFactory> factory, Object handler) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException {
            Class<?>[] handlerInterfaces = ClassUtils.getAllInterfaces(handler);

            Class ifaceClass = null;
            Class<TProcessor> processorClass = null;
            Class serviceClass = null;

            for (Class<?> handlerInterfaceClass : handlerInterfaces) {
                if (!handlerInterfaceClass.getName().endsWith("$Iface")) {
                    continue;
                }

                serviceClass = handlerInterfaceClass.getDeclaringClass();

                if (serviceClass == null) {
                    continue;
                }

                for (Class<?> innerClass : serviceClass.getDeclaredClasses()) {
                    if (!innerClass.getName().endsWith("$Processor")) {
                        continue;
                    }

                    if (!TProcessor.class.isAssignableFrom(innerClass)) {
                        continue;
                    }

                    if (ifaceClass != null) {
                        throw new IllegalStateException("Multiple Thrift Ifaces defined on handler");
                    }

                    ifaceClass = handlerInterfaceClass;
                    processorClass = (Class<TProcessor>) innerClass;
                    break;
                }
            }

            if (ifaceClass == null) {
                throw new IllegalStateException("No Thrift Ifaces found on handler");
            }

            handler = wrapHandler(ifaceClass, handler);

            Constructor<TProcessor> processorConstructor = processorClass.getConstructor(ifaceClass);

            TProcessor processor = BeanUtils.instantiateClass(processorConstructor, handler);

            TServlet servlet;
            if (TProtocolFactory.class.equals(factory)) {
                servlet = getServlet(processor, protocolFactory);
            } else {
                servlet = getServlet(processor, factory.newInstance());
            }

            String servletBeanName = handler.getClass().getSimpleName() + "Servlet";

            ServletRegistration.Dynamic registration = servletContext.addServlet(servletBeanName, servlet);

            if (urls != null && urls.length > 0) {
                registration.addMapping(urls);
            } else {
                registration.addMapping("/" + serviceClass.getSimpleName());
            }
        }

        protected TServlet getServlet(TProcessor processor, TProtocolFactory protocolFactory) {
            return new TServlet(processor, protocolFactory);
        }

        protected <T> T wrapHandler(Class<T> interfaceClass, T handler) {
            ProxyFactory proxyFactory = new ProxyFactory(interfaceClass, new SingletonTargetSource(handler));

            thriftConfigurer.configureProxyFactory(proxyFactory);

            //TODO remove from here?
            proxyFactory.setFrozen(true);
            return (T) proxyFactory.getProxy();
        }
    }
}
