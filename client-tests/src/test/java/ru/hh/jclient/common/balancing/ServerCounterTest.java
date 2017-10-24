package ru.hh.jclient.common.balancing;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ServerCounterTest {

  @Test
  public void onAcquire() throws Exception {
    ServerCounter counter = new ServerCounter();

    counter.onAcquire();

    assertEquals(1, counter.getRequests());
    assertEquals(0, counter.getFails());

    assertEquals(1, counter.getRequests());
    assertEquals(0, counter.getFails());
  }

  @Test
  public void onAcquireRelease() throws Exception {
    ServerCounter counter = new ServerCounter();

    counter.onAcquire();
    counter.onRelease(false);

    assertEquals(0, counter.getRequests());
    assertEquals(0, counter.getFails());
  }

  @Test
  public void onAcquireReleaseWithFail() throws Exception {
    ServerCounter counter = new ServerCounter();

    counter.onAcquire();
    counter.onRelease(true);

    assertEquals(0, counter.getRequests());
    assertEquals(1, counter.getFails());
  }

  @Test
  public void onAcquireReleaseFromTwoThreads() throws Exception {
    ServerCounter counter = new ServerCounter();

    int numOfRequests = 1_000_000;
    Runnable acquireReleaseTask = () -> onAcquireRelease(counter, numOfRequests);

    int tests = 100;
    for (int t = 1; t <= tests; t++) {
      long start = currentTimeMillis();
      Thread thread = new Thread(acquireReleaseTask);
      thread.start();

      onAcquireRelease(counter, numOfRequests);

      thread.join();

      assertCounter(counter, 0, 0, numOfRequests * 2, 0);

      System.out.println("finished iteration " + t + " out of " + tests + " in " + (currentTimeMillis() - start) + " ms");

      counter.reset();
    }
  }

  private static void onAcquireRelease(ServerCounter counter, int times) {
    for (int i = 0; i < times; i++) {
      counter.onAcquire();
      counter.onRelease(false);
    }
  }

  public static void assertCounter(ServerCounter counter, int requests, int fails, long totalRequests, long totalFails) {
    assertEquals("requests", requests, counter.getRequests());
    assertEquals("fails", fails, counter.getFails());
  }
}
