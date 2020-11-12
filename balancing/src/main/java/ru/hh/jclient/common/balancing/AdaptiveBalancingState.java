package ru.hh.jclient.common.balancing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

public class AdaptiveBalancingState extends BalancingState {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdaptiveBalancingState.class);

  private Iterator<ServerEntry> serverEntryIterator;
  private boolean adaptiveFailed;


  public AdaptiveBalancingState(Upstream upstream) {
    super(upstream);
  }

  @Override
  public void acquireServer() {
    if (!adaptiveFailed) {
      try {
        setCurrentServer(acquireAdaptiveServer());
        return;
      } catch (RuntimeException e) {
        LOGGER.error("failed to acquire adaptive servers, falling back to nonadaptive", e);
        adaptiveFailed = true;
      }
    }
    super.acquireServer();
  }

  @Override
  public void releaseServer(long timeToLastByteMicros, boolean isServerError) {
    if (isServerAvailable()) {
      upstream.releaseServer(getCurrentServer().getIndex(), isServerError, timeToLastByteMicros, !adaptiveFailed);
    }
  }

  private ServerEntry acquireAdaptiveServer() {
    if (serverEntryIterator == null) {
      List<ServerEntry> entries = upstream.acquireAdaptiveServers();
      serverEntryIterator = entries.iterator();
    }
    return serverEntryIterator.next();
  }
}
