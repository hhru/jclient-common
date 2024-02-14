package ru.hh.jclient.common.balancing;

import java.util.List;
import static java.util.stream.Collectors.toList;
import java.util.stream.IntStream;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

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
    assertEquals(balancedServers.size(), retriesCount);
    assertEquals(balancedServers.get(0).intValue(), 0);
    assertEquals(balancedServers.get(1).intValue(), 0);
    assertEquals(balancedServers.get(2).intValue(), 0);
  }

  @Test
  public void shouldPickAsMuchAsRequested() {
    int retriesCount = 5;
    var servers = generateServers(3);
    var balancedServers = AdaptiveBalancingStrategy.getServers(servers, retriesCount);
    assertEquals(balancedServers.size(), retriesCount);
    assertEquals(balancedServers.get(0), balancedServers.get(3));
    assertEquals(balancedServers.get(1), balancedServers.get(4));
  }

  private static List<Server> generateServers(int n) {
    return IntStream
        .range(0, n)
        .mapToObj(i -> new Server("test" + i, i, null))
        .toList();
  }
}
