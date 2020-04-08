package ru.hh.jclient.common.balancing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
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
      float currentLoad = (float) server.getRequests() / server.getWeight();
      float statLoad = (float) server.getStatsRequests() / server.getWeight();
      Weight weight = new Weight(isDifferentDC, triedRacks.contains(server.getRack()), currentLoad, statLoad);

      LOGGER.debug("static balancer stats for {}, differentDC:{}, sameRack:{}, load:{}, stat_load:{}", server,
        weight.isDifferentDC(), weight.isSameRack(), weight.getCurrentLoad(), weight.getStatLoad());

      if (!excludedServers.contains(index) && (minIndex < 0 || minWeight.compareTo(weight) > 0)) {
        minIndex = index;
        minWeight = weight;
      }
    }

    if (minIndex != -1) {
      LOGGER.debug("static balancer pick for {}, differentDC:{}, sameRack:{}, load:{}, stat_load:{}", minIndex,
        minWeight.isDifferentDC(), minWeight.isSameRack(), minWeight.getCurrentLoad(), minWeight.getStatLoad());
    } else {
      LOGGER.debug("no server available");
    }
    return minIndex;
  }

  private BalancingStrategy() {
  }

  private static class Weight implements Comparable<Weight> {
    private static final Comparator<Weight> weightComparator = Comparator.comparing(Weight::isDifferentDC)
        .thenComparing(Weight::isSameRack)
        .thenComparingDouble(Weight::getCurrentLoad)
        .thenComparingDouble(Weight::getStatLoad);
    private final boolean differentDC;
    private final boolean sameRack;
    private final float currentLoad;
    private final float statLoad;

    Weight(boolean differentDC, boolean sameRack, float currentLoad, float statLoad) {
      this.differentDC = differentDC;
      this.sameRack = sameRack;
      this.currentLoad = currentLoad;
      this.statLoad = statLoad;
    }

    public boolean isDifferentDC() {
      return differentDC;
    }

    public boolean isSameRack() {
      return sameRack;
    }

    public float getCurrentLoad() {
      return currentLoad;
    }

    public float getStatLoad() {
      return statLoad;
    }

    @Override
    public int compareTo(@Nonnull Weight other) {
      return weightComparator.compare(this, other);
    }
  }
}
