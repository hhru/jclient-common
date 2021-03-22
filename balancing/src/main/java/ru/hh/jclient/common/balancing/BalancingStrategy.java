package ru.hh.jclient.common.balancing;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class BalancingStrategy {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingStrategy.class);

  static int getLeastLoadedServer(List<Server> servers, Set<Integer> excludedServers, String datacenter, boolean allowCrossDCRequests) {
    int minIndex = -1;
    Weight minWeight = null;

    long currentTime = System.currentTimeMillis();
    long warmupTimeMs = 30000;

    for (int index = 0; index < servers.size(); index++) {
      Server server = servers.get(index);

      if (server == null) {
        continue;
      }

      float warmupFactor = Optional.ofNullable(server.getMeta().get("startTimestamp")).map(Long::valueOf)
          .map(startTime -> Math.max(0, currentTime - startTime))
          .map(runningTime -> Math.max(1f, (float) runningTime / warmupTimeMs))
          .orElse(1f);

      float loadCorrection = 1 / (warmupFactor + 1);

      boolean isDifferentDC = !Objects.equals(datacenter, server.getDatacenterLowerCased());

      if (isDifferentDC && !allowCrossDCRequests) {
        continue;
      }

      float currentLoad = ((float) server.getRequests() / server.getWeight()) * loadCorrection;
      float statLoad = (float) server.getStatsRequests() / server.getWeight();
      Weight weight = new Weight(isDifferentDC, currentLoad, statLoad);

      LOGGER.debug("static balancer stats for {}, differentDC:{}, load:{}, stat_load:{}", server,
              weight.isDifferentDC(), weight.getCurrentLoad(), weight.getStatLoad());

      if (!excludedServers.contains(index) && (minIndex < 0 || minWeight.compareTo(weight) > 0)) {
        minIndex = index;
        minWeight = weight;
      }
    }

    if (minIndex != -1) {
      LOGGER.debug("static balancer pick for {}, differentDC:{}, load:{}, stat_load:{}", minIndex,
              minWeight.isDifferentDC(), minWeight.getCurrentLoad(), minWeight.getStatLoad());
    } else {
      LOGGER.debug("no server available");
    }
    return minIndex;
  }

  private BalancingStrategy() {
  }

  private static class Weight implements Comparable<Weight> {
    private static final Comparator<Weight> weightComparator = Comparator.comparing(Weight::isDifferentDC)
        .thenComparingDouble(Weight::getCurrentLoad)
        .thenComparingDouble(Weight::getStatLoad);
    private final boolean differentDC;
    private final float currentLoad;
    private final float statLoad;

    Weight(boolean differentDC, float currentLoad, float statLoad) {
      this.differentDC = differentDC;
      this.currentLoad = currentLoad;
      this.statLoad = statLoad;
    }

    public boolean isDifferentDC() {
      return differentDC;
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
