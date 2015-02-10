jclient-common
-----------------

Jclient-common allows to make asynchronous HTTP calls to remote services in Java applications. Heavy-lifting is performed using [Ning Async Http Client](https://github.com/AsyncHttpClient/async-http-client), while library provides user friendly interface that hides unnecessary details.

Java 8 is a requirement to build or use this library.

Client code that uses this library is usually stored together with server code that it calls, in separate maven module. For example see [sofea-client](https://github.com/hhru/hh.ru/tree/master/sofea-jclient). Exception to this rule is when the server code is unavailable - [banner-jclient](https://github.com/hhru/banner-jclient). 

Sample code:

```java
HttpClientBuilder http = ...;
JAXBContext jaxb = JAXBContext.newInstance(Banners.class);
Request request = new RequestBuilder("GET").setUrl("http://myservice/banners").addQueryParam("places", "1,2,3").build();
CompletableFuture<Banners> bannersFuture = http.with(request).expectXml(jaxb, Banners.class).request();
```

For more information see javadoc.  