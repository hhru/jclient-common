package ru.hh.jclient.common.balancing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AdaptiveBalancingStrategy {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdaptiveBalancingStrategy.class);

  static final int WARM_UP_DEFAULT_TIME_MS = 100;
  static final int DOWNTIME_DETECTOR_WINDOW = 100;
  static final int RESPONSE_TIME_TRACKER_WINDOW = 500;
  private static final int lowestHealthPercent = 2;
  private static final int lowestHealth = lowestHealthPercent * DOWNTIME_DETECTOR_WINDOW / 100;

  static List<Integer> getServers(List<Server> servers, int retriesCount) {
    int n = servers.size();
    int count = Math.max(n, retriesCount);
    if (servers.isEmpty()) {
      return Collections.emptyList();
    }

    long[] scores = new long[n];
    int[] ids = new int[n];
    int[] healths = new int[n];

    // gather statistics
    int i = 0;
    long totalTime = 0L;
    boolean isAnyWarmingUp = false;
    for (Server server : servers) {
      ResponseTimeTracker tracker = server.getResponseTimeTracker();
      isAnyWarmingUp |= tracker.isWarmUp();
      healths[i] = server.getDowntimeDetector().successCount();
      long mean = tracker.mean();
      scores[i] = mean;
      totalTime += mean;
      i++;
    }

    // adjust scores based on downtime detector health and response time tracker score
    long total = 0;
    for (int j = 0; j < n; j++) {
      long time = isAnyWarmingUp ? WARM_UP_DEFAULT_TIME_MS : totalTime - scores[j];
      int health = healths[j];
      long score = time * (health <= lowestHealth ? lowestHealth : health);
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("balancer stats for {}, health:{}, time_score:{}, final_score:{}", servers.get(j), health, time, score);
      }
      total += score;
      scores[j] = score;
      ids[j] = j;
    }

    // weighted-randomly pick count elements
    List<Integer> shuffled = new ArrayList<>(count);
    for (int j = n - 1, r = count - 1; j >= 0 && r >= 0; j--, r--) { // index to put new random element
      long pick = ThreadLocalRandom.current().nextLong(total);
      long sum = 0L;
      for (int k = 0; k <= j; k++) { // random index of element to swap
        sum += scores[k];
        if (pick < sum) {
          shuffled.add(ids[k]);
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("balancer pick for {}, {}:{}", servers.get(ids[k]), n - 1 - j, ids[k]);
          }
          total -= scores[k];
          swap(scores, ids, k, j);
          break;
        }
      }
    }

    return shuffled;
  }

  private static void swap(long[] scores, int[] ids, int a, int b) {
    if (a == b) {
      return;
    }

    long c = scores[a];
    scores[a] = scores[b];
    scores[b] = c;

    int d = ids[a];
    ids[a] = ids[b];
    ids[b] = d;
  }
}
