# Apache Thrift Starter for Spring Boot

[![Join the chat at https://gitter.im/aatarasoff/spring-thrift-starter](https://badges.gitter.im/aatarasoff/spring-thrift-starter.svg)](https://gitter.im/aatarasoff/spring-thrift-starter?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Build Status](https://travis-ci.org/aatarasoff/spring-thrift-starter.svg?branch=master)](https://travis-ci.org/aatarasoff/spring-thrift-starter)

Set of cool annotations that helps you building Thrift applications with Spring Boot.

## How to connect the project
```groovy
repositories {
    ...
    maven {
        url = uri("https://maven.pkg.github.com/aatarasoff/spring-thrift-starter")
        credentials {
            username = System.getenv("USERNAME")
            password = System.getenv("TOKEN")
        }
    }
}

compile 'info.developerblog.spring.thrift:spring-thrift-starter:+'
```

For more information, please, look the official github packages [documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package).

## How to use this

### Server-side
Annotation @ThriftController("servlet_path") helps you building server controller for request processing

```java
@ThriftController("/api")
public class TGreetingServiceController implements TGreetingService.Iface {

    @Override
    public String greet(TName name) throws TException {
        // your logic
    }
}
```
### Client-side

#### @ThriftClient

@ThriftClient(serviceId = "registered_service", (path) = "server_handler_path") helps you with multithreaded client with full Spring Cloud support.
```java
@ThriftClient(serviceId = "greeting-service", path = "/api")
TGreetingService.Client client;
```

#### Beans
Thrift clients can also be used as regular beans 

(which can be configured through [app properties](#thrift-client-configuration))

```java
class Service {
    @Autowired
    private TGreetingService.Client client;
}
```


```java
class Service {
    private final TGreetingService.Client client;
    @Autowired
    public Service(TGreetingService.Client client) {
        this.client = client;
    }
}
```

#### @ThriftClientsMap

@ThriftClientsMap(mapperClass) annotation helps to create a string-keyed map of clients for a set of services having the same interface, allowing to define the concrete callee instance at runtime:
```java
@ThriftClientsMap(mapperClass = SampleMapper.class)
Map<String, TGreetingService.Client> clientsMap;
```
Mapper class requirements:
* must extend AbstractThriftClientKeyMapper
* must be registered as a bean in the application context

#### Thrift Client configuration

```yaml
greeting-service:                     #service name
  endpoint: http://localhost:8080/api #direct endpoint
  ribbon:                             #manually ribbon
      listOfServers: localhost:8080
  path: /service                      #general path
  connectTimeout: 1000                #default=1000
  readTimeout: 10000                  #default=30000

thrift.client.max.threads: 10         #default=8
```

If you use service discovery backend (as Eureka or Consul) only path maybe needed.

See tests for better understanding.

### Sleuth support
Since 1.0.0 starter have supported [Spring Cloud Sleuth](https://cloud.spring.io/spring-cloud-sleuth) for tracing.

## Special thanks to

* [@bsideup](https://github.com/bsideup) who inspired me with his [project](https://github.com/bsideup/thrift-spring-boot-starter)
* [@lavcraft](https://github.com/lavcraft) who was helping me when I've been stucked
* [@driver733](https://github.com/driver733) for implementing the [bean registration support](#beans)

## Enjoy!


