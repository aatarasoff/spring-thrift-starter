package info.developerblog.examples.thirft.simpleclient.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.trylogic.spring.boot.thrift.beans.RequestIdLogger;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by aleksandr on 18.02.16.
 */
@Configuration
public class TestConfiguration {

  @Bean
  RequestIdLogger requestIdLogger() {
    return new RequestIdLogger() {
      private String requestId;

      @Override
      protected String getXRequestId(HttpServletRequest request) {
        requestId = super.getXRequestId(request);
        return requestId;
      }
    };
  }
}
