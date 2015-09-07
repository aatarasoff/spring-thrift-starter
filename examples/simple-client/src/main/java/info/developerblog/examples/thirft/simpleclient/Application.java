package info.developerblog.examples.thirft.simpleclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by aleksandr on 01.09.15.
 */
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = { "info.developerblog" })
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
