package ru.hh.jclient.common.balancing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ScheduledExecutorService;

public class ServerTest {

  @Test
  public void testCreateServer() {
    Server server = new Server("test", 1, "rack1", "dc1");

    assertEquals(0, server.getRequests());
    assertEquals(0, server.getStatsRequests());
    assertEquals(0, server.getFails());
    assertEquals(1, server.getWeight());
    assertEquals("rack1", server.getRack());
    assertEquals("dc1", server.getDatacenter());
    assertTrue(server.isActive());
  }

  @Test
  public void testAcquire() {
    Server server = new Server("test", 1, null, null);

    server.acquire();

    assertEquals(0, server.getFails());
    assertEquals(1, server.getRequests());
    assertEquals(1, server.getStatsRequests());
    assertTrue(server.isActive());
  }

  @Test
  public void testAcquireRelease() {
    Server server = new Server("test", 1, null, null);

    server.acquire();
    server.release(false, 100);

    assertEquals(0, server.getFails());
    assertEquals(0, server.getRequests());
    assertEquals(1, server.getStatsRequests());
    assertTrue(server.isActive());
  }

  @Test
  public void testAcquireReleaseWithFail() {
    Server server = new Server("test", 1, null, null);

    server.acquire();
    server.release(true, 100);

    assertEquals(1, server.getFails());
    assertEquals(0, server.getRequests());
    assertEquals(1, server.getStatsRequests());
    assertTrue(server.isActive());
  }

  @Test
  public void testActivateDeactivate() {
    Server server = new Server("test", 1, null, null);

    server.acquire();
    server.release(true, 100);
    server.deactivate(1, mock(ScheduledExecutorService.class));

    assertFalse(server.isActive());
    assertEquals(1, server.getFails());

    server.activate();

    assertTrue(server.isActive());
    assertEquals(0, server.getFails());
  }
}
