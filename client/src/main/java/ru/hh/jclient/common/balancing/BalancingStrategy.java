package ru.hh.jclient.common.balancing;

import java.util.List;
import java.util.Set;

public class BalancingStrategy {

  static int getLeastLoadedServer(List<Server> servers, Set<Integer> excludedServers) {
    int minIndex = -1;
    float minCurrentLoad = 0, minStatLoad = 0;

    for (int index = 0; index < servers.size(); index++) {
      Server server = servers.get(index);

      if (server != null && server.isActive()) {
        float currentLoad = (float) server.getRequests() / server.getWeight();
        float statLoad = (float) server.getStatsRequests() / server.getWeight();
        boolean minLoad = currentLoad < minCurrentLoad || (currentLoad == minCurrentLoad && statLoad < minStatLoad);

        if (!excludedServers.contains(index) && (minIndex < 0 || minLoad)) {
          minIndex = index;
          minStatLoad = statLoad;
          minCurrentLoad = currentLoad;
        }
      }
    }

    return minIndex;
  }
}
