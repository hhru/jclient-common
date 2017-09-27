package ru.hh.jclient.common;

import com.ning.http.client.AsyncHttpClient;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import ru.hh.jclient.common.balancing.UpstreamManager;

public class HttpClientBuilderTest extends HttpClientTestBase {

  private static final String TEST_BACKEND = "backend";
  private static AsyncHttpClient httpClient = mock(AsyncHttpClient.class);

  @Before
  public void setUp() throws Exception {
    withEmptyContext();
  }

  @Test
  public void createHttpClientBuilderWithoutUpstreams() throws Exception {

    http = createHttpClientBuilder(httpClient);

    UpstreamManager manager = http.getUpstreamManager();

    assertNotNull(manager);
    assertEquals(0, manager.getUpstreams().size());

    // todo: test that request is not balanced if no upstreams
  }

  @Test
  public void createHttpClientBuilderWithUpstreams() throws Exception {

    http = createHttpClientBuilder(httpClient, singletonMap(TEST_BACKEND, "|server=server"));

    UpstreamManager manager = http.getUpstreamManager();

    assertNotNull(manager.getUpstream(TEST_BACKEND));
    assertNotNull(manager.getUpstream("http://" + TEST_BACKEND));
    assertNull(manager.getUpstream("missing_upstream"));
  }
}

