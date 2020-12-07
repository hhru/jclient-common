[![Build Status](https://travis-ci.org/hhru/jclient-common.svg?branch=master)](https://travis-ci.org/hhru/jclient-common) 
[![codecov](https://codecov.io/gh/hhru/jclient-common/branch/master/graph/badge.svg)](https://codecov.io/gh/hhru/jclient-common)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=ru.hh.jclient-common%3Ajclient-common-parent&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=ru.hh.jclient-common%3Ajclient-common-parent)

# What is it?
Jclient-common allows to make asynchronous HTTP calls to remote services in Java applications. 

Heavy-lifting is performed using [Async Http Client](https://github.com/AsyncHttpClient/async-http-client), 
while the library provides user friendly interface that hides unnecessary details.

## Requirements

Java 11 is a requirement to build and use this library.

Additionally, if you want to use method `JClientBase.jerseyUrl()`, you have to provide one of the following libraries: 

* `javax.ws.rs:jsr311-api` _(jersey v.1)_
* `javax.ws.rs:javax.ws.rs-api` _(jersey v.2)_

depending on what version of Jersey you use in your application.     

Client code that uses this library is usually stored together with server code that it calls, in separate maven module. 

Sample code:

```java
HttpClientFactory http = ...;
JAXBContext jaxb = JAXBContext.newInstance(Banners.class);
Request request = new RequestBuilder("GET").setUrl("http://myservice/banners").addQueryParam("places", "1,2,3").build();
CompletableFuture<Banners> bannersFuture = http.with(request).expectXml(jaxb, Banners.class).request();
```

# Creating a client

`HttpClientFactoryBuilder` is a builder that provides convenient way to create an instance of `HttpClientFactory`:

```java
HttpClientFactory http = new HttpClientFactoryBuilder(new SingletonStorage<>(() -> new HttpClientContext(Map.of(), Map.of(), List.of())), List.of())
    .withProperties(jClientProperties)
    .withRequestStrategy(new DefaultRequestStrategy())
    .withCallbackExecutor(Runnable::run)
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
See [balancing readme](./balancing/README.md)