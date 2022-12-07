package ru.hh.jclient.common.balancing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
  }

  @Test
  public void testExternalUrlRequestorBuilding() {
    Request request = createRequest();
    when(upstreamManager.getUpstream(UPSTREAM_NAME)).thenReturn(null);

    RequestBalancer requestBalancer = requestBalancerBuilder.build(request, requestExecutor);
    assertTrue(requestBalancer instanceof ExternalUrlRequestor);
  }

  @Test
  public void testUpstreamRequestBalancerBuilding() {
    Request request = createRequest();
    Upstream upstream = mock(Upstream.class);
    when(upstream.getConfig(any())).thenReturn(UpstreamConfig.DEFAULT_CONFIG);
    when(upstreamManager.getUpstream(UPSTREAM_NAME)).thenReturn(upstream);

    RequestBalancer requestBalancer = requestBalancerBuilder.build(request, requestExecutor);
    assertTrue(requestBalancer instanceof UpstreamRequestBalancer);
  }

  private Request createRequest() {
    return new RequestBuilder().setUrl(URL).build();
  }
}
