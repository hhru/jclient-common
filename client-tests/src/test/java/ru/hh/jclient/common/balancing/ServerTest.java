package ru.hh.jclient.common.balancing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ScheduledExecutorService;

public class ServerTest {

  @Test
  public void testCreateServer() throws Exception {
    Server server = new Server("test", 1);

    assertEquals(0, server.getRequests());
    assertEquals(0, server.getStatsRequests());
    assertEquals(0, server.getFails());
    assertEquals(1, server.getWeight());
    assertTrue(server.isActive());
  }

  @Test
  public void testAcquire() throws Exception {
    Server server = new Server("test", 1);

    server.acquire();

    assertEquals(0, server.getFails());
    assertEquals(1, server.getRequests());
    assertEquals(1, server.getStatsRequests());
    assertTrue(server.isActive());
  }

  @Test
  public void testAcquireRelease() throws Exception {
    Server server = new Server("test", 1);

    server.acquire();
    server.release(false);

    assertEquals(0, server.getFails());
    assertEquals(0, server.getRequests());
    assertEquals(1, server.getStatsRequests());
    assertTrue(server.isActive());
  }

  @Test
  public void testAcquireReleaseWithFail() throws Exception {
    Server server = new Server("test", 1);

    server.acquire();
    server.release(true);

    assertEquals(1, server.getFails());
    assertEquals(0, server.getRequests());
    assertEquals(1, server.getStatsRequests());
    assertTrue(server.isActive());
  }

  @Test
  public void testActivateDeactivate() throws Exception {
    Server server = new Server("test", 1);

    server.acquire();
    server.release(true);
    server.deactivate(1, mock(ScheduledExecutorService.class));

    assertFalse(server.isActive());
    assertEquals(1, server.getFails());

    server.activate();

    assertTrue(server.isActive());
    assertEquals(0, server.getFails());
  }
}