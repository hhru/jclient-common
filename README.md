[![Build Status](https://travis-ci.org/hhru/jclient-common.svg?branch=master)](https://travis-ci.org/hhru/jclient-common) [![codecov](https://codecov.io/gh/hhru/jclient-common/branch/master/graph/badge.svg)](https://codecov.io/gh/hhru/jclient-common)

# What is it?
Jclient-common allows to make asynchronous HTTP calls to remote services in Java applications. 

Heavy-lifting is performed using [Async Http Client](https://github.com/AsyncHttpClient/async-http-client), 
while the library provides user friendly interface that hides unnecessary details.

## Requirements

Java 8 is a requirement to build and use this library.

Additionally, if you want to use method `JClientBase.jerseyUrl()`, you have to provide one of the following libraries: 

* `javax.ws.rs:jsr311-api` _(jersey v.1)_
* `javax.ws.rs:javax.ws.rs-api` _(jersey v.2)_

depending on what version of Jersey you use in your application.     

Client code that uses this library is usually stored together with server code that it calls, in separate maven module. 

Sample code:

```java
HttpClientBuilder http = ...;
JAXBContext jaxb = JAXBContext.newInstance(Banners.class);
Request request = new RequestBuilder("GET").setUrl("http://myservice/banners").addQueryParam("places", "1,2,3").build();
CompletableFuture<Banners> bannersFuture = http.with(request).expectXml(jaxb, Banners.class).request();
```

# Creating a client

`HttpClientConfig` is a builder that provides convenient way to create an instance of `HttpClientBuilder`:

```java
HttpClientBuilder http = HttpClientConfig.basedOn(jClientProperties)
    .withCallbackExecutor(callbackExecutor)
    .withStorage(contextStorage)
    .withHostsWithSession(hostsWithSession)
    .withUserAgent("my service")
    .build();
```

Example of jclient properties:

```
jclient.connectionTimeoutMs=1100
jclient.requestTimeoutMs=2100
jclient.readTimeoutMs=-1
jclient.userAgent=hh-xmlback
jclient.hostsWithSession=http://localhost
```

Notice that `readTimeoutMs` is set to `-1`, which means to ignore this setting.

This is because in case when `readTimeout < requestTimeout`, `readTimeout` will be executed earlier than `requestTimeout`,
so if `requestTimeout` is sufficiently large (e.g. for slow requests) it will not work as expected.     

## Load balancing

Jclient provides a way to balance load between separate instances of an upstream server.

Currently two methods are supported:
* weighted least-connection  
* adaptive balancing 

Your application should have connection to Cassandra in order to access configuration of upstreams.

Additionally, you have to include `jclient-common-metrics` artifact:

```xml
<dependency>
    <groupId>ru.hh.jclient-common</groupId>
    <artifactId>jclient-common-metrics</artifactId>
    <version>0.1.66</version>
</dependency>
```  

Upstreams are managed by `BalancingUpstreamManager` class.

### Spring configuration

```java
@Configuration
public class JClientConfig {
  
  @Bean
  public UpstreamManager upstreamManager(Session cassandraSession, StatsDSender statsDSender) {
    return BalancingUpstreamManagerFactory.create("service-name", cassandraSession, statsDSender);
  }

  @Bean
  public HttpClientBuilder httpClientBuilder(Properties jClientProperties,
                                             Storage<HttpClientContext> contextStorage,
                                             UpstreamManager upstreamManager) {
    
    return HttpClientConfig.basedOn(jClientProperties)
      .withUpstreamManager(upstreamManager)    
      .withStorage(contextStorage)
      .withCallbackExecutor(Runnable::run)
      .withHostsWithSession(Collections.singletonList(jClientProperties.getProperty("hostsWithSession")))
      .withUserAgent(jClientProperties.getProperty("userAgent"))
      .build();
  }
}
```

Notice that you have to provide an instance of `StatsDSender` to create `UpstreamManager` bean.

This is because `BalancingUpstreamManager` has built-in monitoring of requests and `StatsDSender` is used to send metrics to our monitoring system ([okmeter.io](https://okmeter.io)).

If you use [NaB](https://github.com/hhru/nuts-and-bolts), then you already have `StatsDSender` in your Spring context.

In other case please refer to this [quick-guide](https://github.com/hhru/metrics#quick-guide).
