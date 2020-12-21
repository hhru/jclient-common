package ru.hh.jclient.common.balancing;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ServerTest {

  @Test
  public void testCreateServer() {
    Server server = new Server("test", 1,  "DC1");

    assertEquals(0, server.getRequests());
    assertEquals(0, server.getStatsRequests());
    assertEquals(0, server.getFails());
    assertEquals(1, server.getWeight());
    assertEquals("dc1", server.getDatacenterLowerCased());
  }

  @Test
  public void testAcquire() {
    Server server = new Server("test", 1, null);

    server.acquire();

    assertEquals(0, server.getFails());
    assertEquals(1, server.getRequests());
    assertEquals(1, server.getStatsRequests());
  }

  @Test
  public void testAcquireRelease() {
    Server server = new Server("test", 1,  null);

    server.acquire();
    server.release(false);

    assertEquals(0, server.getFails());
    assertEquals(0, server.getRequests());
    assertEquals(1, server.getStatsRequests());
  }

  @Test
  public void testAcquireReleaseWithFail() {
    Server server = new Server("test", 1,  null);

    server.acquire();
    server.release(true);

    assertEquals(1, server.getFails());
    assertEquals(0, server.getRequests());
    assertEquals(1, server.getStatsRequests());
  }

}
