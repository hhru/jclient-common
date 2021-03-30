package ru.hh.jclient.common.balancing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongSupplier;

final class BalancingStrategy {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingStrategy.class);

  static int getLeastLoadedServer(List<Server> servers, Set<Integer> excludedServers, String datacenter, boolean allowCrossDCRequests,
                                  LongSupplier currentTimeMillisProvider) {
    int minIndex = -1;
    Weight minWeight = null;

    for (int index = 0; index < servers.size(); index++) {
      Server server = servers.get(index);

      if (server == null) {
        continue;
      }

      boolean isDifferentDC = !Objects.equals(datacenter, server.getDatacenterLowerCased());

      if (isDifferentDC && !allowCrossDCRequests) {
        continue;
      }

      Weight weight = new Weight(isDifferentDC, server, servers, currentTimeMillisProvider);

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

  private static final class Weight implements Comparable<Weight> {
    private static final Comparator<Weight> weightComparator = Comparator.comparing(Weight::isDifferentDC)
        .thenComparingDouble(Weight::getCurrentLoad)
        .thenComparingDouble(Weight::getStatLoad);
    private final boolean differentDC;
    private final float currentLoad;
    private final float statLoad;

    Weight(boolean differentDC, Server server, Collection<Server> currentServers, LongSupplier currentTimeMillisProvider) {
      this.differentDC = differentDC;
      this.currentLoad = server.getCurrentLoad();
      this.statLoad = server.getStatLoad(currentServers, currentTimeMillisProvider);
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
