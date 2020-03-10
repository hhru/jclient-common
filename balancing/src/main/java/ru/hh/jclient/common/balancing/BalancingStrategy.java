package ru.hh.jclient.common.balancing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class BalancingStrategy {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingStrategy.class);

  static int getLeastLoadedServer(List<Server> servers, Set<Integer> excludedServers, String datacenter, boolean allowCrossDCRequests) {
    int minIndex = -1;
    Weight minWeight = null;

    for (int index = 0; index < servers.size(); index++) {
      Server server = servers.get(index);

      if (server == null || !server.isActive()) {
        continue;
      }

      boolean isDifferentDC = !Objects.equals(datacenter, server.getDatacenter());

      if (isDifferentDC && !allowCrossDCRequests) {
        continue;
      }

      Set<String> triedRacks = Collections.emptySet();
      if (!excludedServers.isEmpty()) {
        triedRacks = excludedServers.stream().map(servers::get).filter(Objects::nonNull).map(Server::getAddress).collect(Collectors.toSet());
      }

      Weight weight = new Weight();
      weight.differentDC = isDifferentDC;
      weight.sameRack = triedRacks.contains(server.getRack());
      weight.currentLoad = (float) server.getRequests() / server.getWeight();
      weight.statLoad = (float) server.getStatsRequests() / server.getWeight();

      LOGGER.debug("static balancer stats for {}, differentDC:{}, sameRack:{}, load:{}, stat_load:{}", server,
        weight.differentDC, weight.sameRack, weight.currentLoad, weight.statLoad);

      if (!excludedServers.contains(index) && (minIndex < 0 || minWeight.compareTo(weight) > 0)) {
        minIndex = index;
        minWeight = weight;
      }
    }

    if (minIndex != -1) {
      LOGGER.debug("static balancer pick for {}, differentDC:{}, sameRack:{}, load:{}, stat_load:{}", minIndex,
        minWeight.differentDC, minWeight.sameRack, minWeight.currentLoad, minWeight.statLoad);
    } else {
      LOGGER.debug("no server available");
    }

    return minIndex;
  }

  private BalancingStrategy() {
  }

  private static class Weight implements Comparable<Weight> {
    boolean differentDC;
    boolean sameRack;
    float currentLoad;
    float statLoad;

    @Override
    public int compareTo(Weight other) {
      return Comparator.comparing((Weight weight) -> weight.differentDC)
        .thenComparing(weight -> weight.sameRack)
        .thenComparingDouble(weight -> weight.currentLoad)
        .thenComparingDouble(weight -> weight.statLoad)
        .compare(this, other);
    }
  }
}
