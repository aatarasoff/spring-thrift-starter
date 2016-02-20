package info.developerblog.examples.thirft.proxyclient.jdkproxy;

import org.apache.thrift.TException;

public interface JdkProxyGreetingService {

    String getGreeting(String lastName, String firstName) throws TException;
}
