package ru.hh.jclient.common.balancing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

final class BalancingStrategy {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingStrategy.class);

  static int getLeastLoadedServer(List<Server> servers, Set<Integer> excludedServers) {
    int minIndex = -1;
    float minCurrentLoad = 0, minStatLoad = 0;

    for (int index = 0; index < servers.size(); index++) {
      Server server = servers.get(index);

      if (server != null && server.isActive()) {
        float currentLoad = (float) server.getRequests() / server.getWeight();
        float statLoad = (float) server.getStatsRequests() / server.getWeight();
        boolean minLoad = currentLoad < minCurrentLoad || (currentLoad == minCurrentLoad && statLoad < minStatLoad);
        LOGGER.debug("static balancer stats for {}, load:{}, stat_load:{}", server, currentLoad, statLoad);

        if (!excludedServers.contains(index) && (minIndex < 0 || minLoad)) {
          minIndex = index;
          minStatLoad = statLoad;
          minCurrentLoad = currentLoad;
        }
      }
    }

    LOGGER.debug("static balancer pick for {}, load:{}, stat_load:{}", minIndex, minCurrentLoad, minStatLoad);
    return minIndex;
  }

  private BalancingStrategy() {
  }
}
