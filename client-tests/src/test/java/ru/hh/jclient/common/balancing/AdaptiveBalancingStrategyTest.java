package ru.hh.jclient.common.balancing;

import org.junit.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public class AdaptiveBalancingStrategyTest extends AbstractBalancingStrategyTest {
  @Test
  public void shouldPickLessThanAll() {
    int retriesCount = 2;
    var balancedServers = AdaptiveBalancingStrategy.getServers(generate3Servers(), retriesCount);
    assertEquals(retriesCount, balancedServers.size());
  }

  @Test
  public void shouldPickDifferent() {
    var servers = generate3Servers();
    var balancedServers = AdaptiveBalancingStrategy.getServers(servers, servers.size());
    assertEquals(List.of(0, 1, 2), balancedServers.stream().sorted().collect(toList()));
  }

  private static List<Server> generate3Servers() {
    return List.of(
        new Server("test1", 1, null),
        new Server("test2", 1, null),
        new Server("test3", 1, null)
    );
  }
}
