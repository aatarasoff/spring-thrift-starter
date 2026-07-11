package info.developerblog.examples.thirft.simpleclient;

import info.developerblog.spring.thrift.client.ThriftClientHeaderCustomizer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Verifies that a registered {@link ThriftClientHeaderCustomizer} propagates its headers
 * for both regular {@code @ThriftClient} proxies and clients obtained via
 * {@code @ThriftClientsMap}.
 */
@SpringBootTest(
        classes = {SimpleClientApplication.class, ThriftClientHeaderCustomizerTests.TestConfig.class},
        webEnvironment = RANDOM_PORT
)
public class ThriftClientHeaderCustomizerTests {

    static final String TEST_HEADER_NAME = "X-Test-Header";
    static final String TEST_HEADER_VALUE = "test-value";

    @TestConfiguration
    static class TestConfig {

        @Bean
        public ThriftClientHeaderCustomizer testHeaderCustomizer() {
            return () -> Map.of(TEST_HEADER_NAME, TEST_HEADER_VALUE);
        }

        @Bean
        public CapturingFilter capturingFilter() {
            return new CapturingFilter();
        }

        @Bean
        public FilterRegistrationBean<CapturingFilter> capturingFilterRegistration(CapturingFilter filter) {
            FilterRegistrationBean<CapturingFilter> bean = new FilterRegistrationBean<>(filter);
            bean.addUrlPatterns("/api");
            return bean;
        }
    }

    static class CapturingFilter implements Filter {
        volatile String lastTestHeader;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            lastTestHeader = ((HttpServletRequest) request).getHeader(TEST_HEADER_NAME);
            chain.doFilter(request, response);
        }
    }

    @Autowired
    GreetingService greetingService;

    @Autowired
    CapturingFilter capturingFilter;

    @BeforeEach
    void resetFilter() {
        capturingFilter.lastTestHeader = null;
    }

    @Test
    void thriftClientPropagatesCustomHeader() throws Exception {
        greetingService.getGreeting("Smith", "John");
        assertEquals(TEST_HEADER_VALUE, capturingFilter.lastTestHeader,
                "@ThriftClient call should propagate the custom header");
    }

    @Test
    void thriftClientsMapPropagatesCustomHeader() throws Exception {
        greetingService.getGreetingForKey("key1", "Doe", "John");
        assertEquals(TEST_HEADER_VALUE, capturingFilter.lastTestHeader,
                "@ThriftClientsMap call should propagate the custom header");
    }
}
