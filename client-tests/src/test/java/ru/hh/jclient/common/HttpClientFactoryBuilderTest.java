package ru.hh.jclient.common;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static ru.hh.jclient.common.HttpClientFactoryBuilder.DEFAULT_BALANCING_REQUESTS_LOG_LEVEL;
import ru.hh.jclient.common.balancing.BalancingRequestStrategy;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.jclient.common.balancing.JClientInfrastructureConfig;
import ru.hh.jclient.common.balancing.RequestBalancerBuilder;
import ru.hh.jclient.common.balancing.UpstreamManager;
import ru.hh.jclient.common.util.storage.SingletonStorage;
import ru.hh.jclient.common.util.storage.Storage;
import ru.hh.jclient.consul.TestUpstreamConfigService;
import ru.hh.jclient.consul.TestUpstreamService;

public class HttpClientFactoryBuilderTest {

  @Test
  public void testImmutableByDefault() {
    var initial = new HttpClientFactoryBuilder(mock(Storage.class), List.of());
    testBuilderMethods(initial, not(equalTo(initial)));
  }

  @Test
  public void testRequestBalancerBuilderProperties() {
    String levelOverride = "INFO";
    assertNotEquals(levelOverride, DEFAULT_BALANCING_REQUESTS_LOG_LEVEL);

    Properties properties = new Properties();
    properties.put("balancingRequestsLogLevel", levelOverride);

    JClientInfrastructureConfig infrastructureConfig = mock(JClientInfrastructureConfig.class);
    UpstreamManager upstreamManager = new BalancingUpstreamManager(null, null, Set.of(), infrastructureConfig, false);
    HttpClientFactoryBuilder httpClientFactoryBuilder = new HttpClientFactoryBuilder(
        new SingletonStorage<>(() -> new HttpClientContext(Map.of(), Map.of(), List.of())),
        List.of()
    )
        .withRequestStrategy(new BalancingRequestStrategy(upstreamManager, new TestUpstreamService(), new TestUpstreamConfigService()))
        .withCallbackExecutor(Executors.newSingleThreadExecutor());

    HttpClientFactory httpClientFactory = httpClientFactoryBuilder.withProperties(properties).build();

    Request request = new RequestBuilder(JClientBase.HTTP_GET).setUrl("http://nevajno").build();
    HttpClient client = httpClientFactory.with(request);

    RequestBalancerBuilder requestBalancerBuilder = client.configureRequestEngine(RequestBalancerBuilder.class);
    assertEquals(requestBalancerBuilder.getBalancingRequestsLogLevel(), levelOverride);
  }

  @Test
  public void testMultiplierApplication() {
    Properties properties = new Properties();
    properties.put("requestTimeoutMs", "1000");
    properties.put("timeoutMultiplier", "5.0");
    var initial = new HttpClientFactoryBuilder(mock(Storage.class), List.of()).withCallbackExecutor(Executors.newSingleThreadExecutor());
    var client = initial.withProperties(properties).build();
    assertEquals(5000, client.getHttp().getConfig().getRequestTimeout());
    assertNotEquals(new DefaultAsyncHttpClientConfig.Builder().build().getRequestTimeout(), client.getHttp().getConfig().getRequestTimeout());
  }

  private void testBuilderMethods(HttpClientFactoryBuilder initial, Matcher<Object> matcher) {
    var methods = HttpClientFactoryBuilder.class.getDeclaredMethods();
    Stream
        .of(methods)
        .filter(method -> !Modifier.isStatic(method.getModifiers())
            && Modifier.isPublic(method.getModifiers())
            && HttpClientFactoryBuilder.class.equals(method.getReturnType())
            && method.getParameterTypes().length == 1)
        .forEach(builderMethod -> {
          try {
            var actual = builderMethod.invoke(initial, generateArgument(builderMethod));
            assertThat(actual, matcher);
          } catch (Exception e) {

            fail("Fail to invoke method " + builderMethod.getName() + ":" + System.lineSeparator() + ExceptionUtils.getStackTrace((e)));
          }
        });
  }

  private Object generateArgument(Method method) {
    var argClass = method.getParameterTypes()[0];
    if (Object.class.equals(argClass)) {
      return new DefaultAsyncHttpClientConfig.Builder().build();
    } else if (boolean.class.equals(argClass)) {
      return true;
    } else if (double.class.equals(argClass)) {
      return 1.1;
    } else if (int.class.equals(argClass)) {
      return 1;
    } else if (String.class.equals(argClass)) {
      return "test";
    } else if (Collection.class.equals(argClass)) {
      return List.of();
    } else {
      return mock(argClass);
    }
  }
}
