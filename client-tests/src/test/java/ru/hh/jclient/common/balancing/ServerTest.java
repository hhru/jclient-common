package ru.hh.jclient.common.balancing;

import java.util.Collections;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ServerTest {

  @Test
  public void testUpdateServer() {
    Server server = new Server("test", 1,  "DC1");
    for (int i = 0; i < 100; i++) {
      server.acquire();
    }
    assertEquals(100, server.getStatsRequests());

    server.update(5, Collections.emptyMap(), Collections.emptyList());
    assertEquals(500, server.getStatsRequests());
  }

  @Test
  public void testCreateServer() {
    Server server = new Server("test", 1,  "DC1");

    assertEquals(0, server.getCurrentRequests());
    assertEquals(0, server.getStatsRequests());
    assertEquals(1, server.getWeight());
    assertEquals("DC1", server.getDatacenter());
  }

  @Test
  public void testAcquire() {
    Server server = new Server("test", 1, null);

    server.acquire();

    assertEquals(1, server.getCurrentRequests());
    assertEquals(1, server.getStatsRequests());
  }

  @Test
  public void testAcquireRelease() {
    Server server = new Server("test", 1,  null);

    server.acquire();
    server.release(false);

    assertEquals(0, server.getCurrentRequests());
    assertEquals(1, server.getStatsRequests());
  }

  @Test
  public void testAcquireReleaseWithFail() {
    Server server = new Server("test", 1,  null);

    server.acquire();
    server.release(false);

    assertEquals(0, server.getCurrentRequests());
    assertEquals(1, server.getStatsRequests());
  }

}
