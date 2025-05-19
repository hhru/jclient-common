package ru.hh.jclient.common.balancing;

import java.util.List;
import java.util.Random;
import static java.util.stream.Collectors.toList;
import java.util.stream.IntStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static ru.hh.jclient.common.balancing.AdaptiveBalancingStrategy.RESPONSE_TIME_TRACKER_WINDOW;

public class AdaptiveBalancingStrategyTest extends AbstractBalancingStrategyTest {
  @Test
  public void shouldPickLessThanAll() {
    int retriesCount = 2;
    var servers = generateServers(3);
    var balancedServers = AdaptiveBalancingStrategy.getServers(servers, retriesCount);
    assertEquals(retriesCount, balancedServers.size());
  }

  @Test
  public void shouldPickDifferent() {
    var servers = generateServers(3);
    var balancedServers = AdaptiveBalancingStrategy.getServers(servers, servers.size());
    assertEquals(List.of(0, 1, 2), balancedServers.stream().sorted().collect(toList()));
  }

  @Test
  public void shouldPickSameServerSeveralTimes() {
    int retriesCount = 3;
    var servers = generateServers(1);
    var balancedServers = AdaptiveBalancingStrategy.getServers(servers, retriesCount);
    assertEquals(List.of(0, 0, 0), balancedServers);
  }

  @Test
  public void shouldPickAsMuchAsRequested() {
    int retriesCount = 5;
    var servers = generateServers(3);
    var balancedServers = AdaptiveBalancingStrategy.getServers(servers, retriesCount);

    assertEquals(retriesCount, balancedServers.size());
    assertEquals("Extra servers should be repeated in the same order", balancedServers.subList(0, 2), balancedServers.subList(3, 5));
  }

  @Test
  public void shouldReturnEmpty() {
    assertEquals(List.of(), AdaptiveBalancingStrategy.getServers(generateServers(3), 0));
    assertEquals(List.of(), AdaptiveBalancingStrategy.getServers(List.of(), 2));
  }

  @Test
  public void shouldWarmUp() {
    int retriesCount = 2;
    var servers = generateServers(2);
    var balancedServers = AdaptiveBalancingStrategy.getServers(servers, retriesCount);

    var responseTimeTracker1 = servers.get(balancedServers.get(0)).getResponseTimeTracker();
    var responseTimeTracker2 = servers.get(balancedServers.get(1)).getResponseTimeTracker();
    assertTrue(responseTimeTracker1.isWarmUp());
    assertTrue(responseTimeTracker2.isWarmUp());

    var random = new Random();
    for (int i = 0; i < RESPONSE_TIME_TRACKER_WINDOW; i++) {
      responseTimeTracker1.time(random.nextInt(100, 200));
    }

    balancedServers = AdaptiveBalancingStrategy.getServers(servers, retriesCount);
    boolean warmUp1 = servers.get(balancedServers.get(0)).getResponseTimeTracker().isWarmUp();
    boolean warmUp2 = servers.get(balancedServers.get(1)).getResponseTimeTracker().isWarmUp();
    assertNotEquals("Only one server should be warmed up", warmUp1, warmUp2);
  }

  private static List<Server> generateServers(int n) {
    return IntStream
        .range(0, n)
        .mapToObj(i -> new Server("test" + i, null, i, null))
        .toList();
  }
}
