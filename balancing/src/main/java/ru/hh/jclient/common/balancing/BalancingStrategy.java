package ru.hh.jclient.common.balancing;

import java.time.Clock;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BalancingStrategy {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingStrategy.class);

  static int getLeastLoadedServer(List<Server> servers, Set<Integer> excludedServers, String currentDatacenter, boolean allowCrossDCRequests,
                                  Clock clock) {
    int minIndex = -1;
    Weight minWeight = null;
    String[] serverStatLog = LOGGER.isTraceEnabled() ? new String[servers.size()] : null;
    for (int index = 0; index < servers.size(); index++) {
      Server server = servers.get(index);

      if (server == null) {
        continue;
      }

      boolean isDifferentDC = !Objects.equals(currentDatacenter, server.getDatacenter());

      if (isDifferentDC && !allowCrossDCRequests) {
        continue;
      }

      float statLoad = server.getStatLoad(servers, clock);
      Weight weight = new Weight(excludedServers.contains(index), isDifferentDC, statLoad);
      if (LOGGER.isTraceEnabled()) {
        serverStatLog[index] = "{static balancer stats for " + server
            + ", excluded:" + weight.isExcluded()
            + ", differentDC:" + weight.isDifferentDC()
            + ", load:" + weight.getLoad() + '}';
      }

      if (minIndex < 0 || minWeight.compareTo(weight) > 0) {
        minIndex = index;
        minWeight = weight;
      }
    }

    if (minIndex != -1) {
      LOGGER.debug("static balancer pick excluded:{} differentDC:{}, load:{} for server idx={}:{}{}",
          minWeight.isExcluded(), minWeight.isDifferentDC(), minWeight.getLoad(),
          minIndex, servers.get(minIndex),
          !LOGGER.isTraceEnabled() ? ""
              :(" of " + Arrays.toString(serverStatLog) + " with excluded idx=" + excludedServers)
      );
    } else {
      LOGGER.debug("no server available");
    }
    return minIndex;
  }

  private BalancingStrategy() {
  }

  private static final class Weight implements Comparable<Weight> {
    private static final Comparator<Weight> weightComparator = Comparator.comparing(Weight::isExcluded).thenComparing(Weight::isDifferentDC)
        .thenComparingDouble(Weight::getLoad);
    private final boolean excluded;
    private final boolean differentDC;
    private final float load;

    Weight(boolean excluded, boolean differentDC, float load) {
      this.excluded = excluded;
      this.differentDC = differentDC;
      this.load = load;
    }

    public boolean isDifferentDC() {
      return differentDC;
    }

    public float getLoad() {
      return load;
    }

    public boolean isExcluded() {
      return excluded;
    }

    @Override
    public int compareTo(@Nonnull Weight other) {
      return weightComparator.compare(this, other);
    }
  }
}
