package info.developerblog.examples.thirft.simpleclient.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Created by aleksandr on 18.02.16.
 */
@TestConfiguration
@EnableAspectJAutoProxy
public class TestAspectConfiguration {

    @Bean
    public CountingAspect countingAspect(){
        return new CountingAspect();
    }

}
