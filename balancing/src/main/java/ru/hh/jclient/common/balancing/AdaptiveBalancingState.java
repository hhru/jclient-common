package ru.hh.jclient.common.balancing;

import java.util.Iterator;
import java.util.List;
import ru.hh.jclient.common.balancing.config.BalancingStrategyType;

public class AdaptiveBalancingState extends BalancingState {
  private Iterator<ServerEntry> serverEntryIterator;

  public AdaptiveBalancingState(Upstream upstream, String profile) {
    super(upstream, profile);
  }

  @Override
  public void acquireServer() {
    setCurrentServer(acquireAdaptiveServer());
  }

  @Override
  public void releaseServer(long timeToLastByteMillis, boolean isServerError) {
    if (isServerAvailable()) {
      upstream.releaseServer(getCurrentServer().getIndex(), !getTriedServers().isEmpty(), isServerError, timeToLastByteMillis, true);
    }
  }

  private ServerEntry acquireAdaptiveServer() {
    if (serverEntryIterator == null) {
      List<ServerEntry> entries = upstream.acquireAdaptiveServers(profile);
      serverEntryIterator = entries.iterator();
    }
    return serverEntryIterator.hasNext() ? serverEntryIterator.next() : null;
  }

  @Override
  BalancingStrategyType getBalancingStrategyType() {
    return BalancingStrategyType.ADAPTIVE;
  }
}
