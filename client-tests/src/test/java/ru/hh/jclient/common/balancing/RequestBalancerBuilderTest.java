package ru.hh.jclient.common.balancing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.hh.jclient.common.HttpClientFactoryBuilder.DEFAULT_BALANCING_REQUESTS_LOG_LEVEL;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.RequestStrategy;

public class RequestBalancerBuilderTest {

  private static final String UPSTREAM_NAME = "test-upstream";
  private static final String URL = "https://" + UPSTREAM_NAME;

  private RequestBalancerBuilder requestBalancerBuilder;
  private RequestStrategy.RequestExecutor requestExecutor;
  private UpstreamManager upstreamManager;

  @Before
  public void setUp() {
    requestExecutor = mock(RequestStrategy.RequestExecutor.class);
    upstreamManager = mock(UpstreamManager.class);
    requestBalancerBuilder = new RequestBalancerBuilder(upstreamManager, null);
    requestBalancerBuilder.withBalancingRequestsLogLevel(DEFAULT_BALANCING_REQUESTS_LOG_LEVEL);
  }

  @Test
  public void testExternalUrlRequestorBuilding() {
    Request request = createRequest();
    when(upstreamManager.getUpstream(URL)).thenReturn(null);

    RequestBalancer requestBalancer = requestBalancerBuilder.build(request, requestExecutor);
    assertTrue(requestBalancer instanceof ExternalUrlRequestor);
  }

  @Test
  public void testExternalMaxTriesBuilding() {
    Request request = createRequest();
    when(upstreamManager.getUpstream(URL)).thenReturn(null);

    RequestBalancer requestBalancer = requestBalancerBuilder.withMaxTries(13).build(request, requestExecutor);
    assertTrue(requestBalancer instanceof ExternalUrlRequestor);
    assertEquals(13, requestBalancer.maxTries);
  }

  @Test
  public void testUpstreamRequestBalancerBuilding() {
    Request request = createRequest();
    Upstream upstream = mock(Upstream.class);
    when(upstream.isEnabled()).thenReturn(true);
    when(upstream.getConfig(any())).thenReturn(UpstreamConfig.DEFAULT_CONFIG);
    when(upstreamManager.getUpstream(UPSTREAM_NAME, null)).thenReturn(upstream);

    RequestBalancer requestBalancer = requestBalancerBuilder.build(request, requestExecutor);
    assertTrue(requestBalancer instanceof UpstreamRequestBalancer);
  }

  @Test
  public void testDisabledUpstreamRequestBalancerBuilding() {
    Request request = createRequest();
    Upstream upstream = mock(Upstream.class);
    when(upstream.isEnabled()).thenReturn(false);
    when(upstream.getConfig(any())).thenReturn(UpstreamConfig.DEFAULT_CONFIG);
    when(upstreamManager.getUpstream(UPSTREAM_NAME, null)).thenReturn(upstream);

    RequestBalancer requestBalancer = requestBalancerBuilder.build(request, requestExecutor);
    assertTrue(requestBalancer instanceof ExternalUrlRequestor);
  }

  private Request createRequest() {
    return new RequestBuilder().setUrl(URL).build();
  }
}
