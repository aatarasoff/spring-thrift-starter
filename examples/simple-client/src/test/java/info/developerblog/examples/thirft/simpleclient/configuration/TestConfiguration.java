package info.developerblog.examples.thirft.simpleclient.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Created by aleksandr on 18.02.16.
 */
@Configuration
@EnableAspectJAutoProxy
public class TestConfiguration {
    @Bean
    public CountingAspect countingAspect(){
        return new CountingAspect();
    }

}
