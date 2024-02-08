package ru.hh.jclient.common.balancing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.jclient.common.balancing.RequestBalancer.WARM_UP_DEFAULT_TIME_MILLIS;

final class AdaptiveBalancingStrategy {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdaptiveBalancingStrategy.class);

  static final int DOWNTIME_DETECTOR_WINDOW = 100;
  static final int RESPONSE_TIME_TRACKER_WINDOW = 500;
  private static final int lowestHealthPercent = 2;
  private static final int lowestHealth = lowestHealthPercent * DOWNTIME_DETECTOR_WINDOW / 100;

  static List<Integer> getServers(List<Server> servers, int retriesCount) {
    int n = servers.size();
    int count = Math.min(n, retriesCount);
    if (servers.isEmpty()) {
      return Collections.emptyList();
    }

    if (servers.size() == 1) {
      return Stream.generate(() -> 0).limit(retriesCount).toList();
    }

    long[] scores = new long[n];
    int[] ids = new int[n];
    int[] healths = new int[n];

    // gather statistics
    int i = 0;
    boolean isAnyWarmingUp = false;
    long min = Long.MAX_VALUE;
    long max = 0;
    for (Server server : servers) {
      healths[i] = server.getDowntimeDetector().successCount();

      var tracker = server.getResponseTimeTracker();
      LOGGER.debug("gathering stats {}, warmUp:{}, time:{}, successCount:{}", server, tracker.isWarmUp(),
          tracker.mean(), server.getDowntimeDetector().successCount());
      if (tracker.isWarmUp()) {
        isAnyWarmingUp = true;
      } else {
        long mean = Math.max(1, tracker.mean());
        scores[i] = mean;
        min = Math.min(min, mean);
        max = Math.max(max, mean);
      }

      i++;
    }

    for (int j = 0; j < n; j++) {
      long time = isAnyWarmingUp ? WARM_UP_DEFAULT_TIME_MILLIS : scores[j];
      scores[j] = isAnyWarmingUp ? time : Math.round((float) min * max / time);
    }

    // adjust scores based on downtime detector health and response time tracker score
    long total = 0;
    for (int j = 0; j < n; j++) {
      long invertedTime = scores[j];
      int health = Math.max(healths[j], lowestHealth);
      long score = invertedTime * health;
      LOGGER.debug("balancer stats for {}, health:{}, inverted_time_score:{}, final_score:{}", servers.get(j), health, invertedTime, score);
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
          LOGGER.debug("balancer pick for {}, {}:{}", servers.get(ids[k]), n - 1 - j, ids[k]);
          total -= scores[k];
          swap(scores, ids, k, j);
          break;
        }
      }
    }

    if (count >= retriesCount) {
      return shuffled;
    }

    var moreIds = new ArrayList<Integer>(shuffled);
    for (int j = 0; j < retriesCount - count; j++) {
      moreIds.add(shuffled.get(j % count));
    }
    return moreIds;
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

  private AdaptiveBalancingStrategy() {
  }
}
