package ru.hh.jclient.common.balancing;

import java.util.HashSet;
import java.util.Set;

public class BalancingState {
  protected final Upstream upstream;
  private final Set<Integer> triedServers;
  private ServerEntry currentServer;

  public BalancingState(Upstream upstream) {
    this.upstream = upstream;
    this.triedServers = new HashSet<>();
  }

  public UpstreamConfig getUpstreamConfig() {
    return upstream.getConfig();
  }

  public boolean isUpstreamEnabled() {
    return upstream.isEnabled();
  }

  public String getUpstreamName() {
    return upstream.getName();
  }

  public ServerEntry getCurrentServer() {
    return currentServer;
  }

  public boolean isServerAvailable() {
    return currentServer != null && currentServer.getIndex() >= 0;
  }

  public void incrementTries() {
    if (isServerAvailable()) {
      triedServers.add(currentServer.getIndex());
      currentServer = null;
    }
  }

  public void acquireServer() {
    setCurrentServer(upstream.acquireServer(getTriedServers()));
  }

  public void releaseServer(long timeToLastByteMicros, boolean isServerError) {
    if (isServerAvailable()) {
      upstream.releaseServer(getCurrentServer().getIndex(), isServerError, timeToLastByteMicros, false);
    }
  }

  private Set<Integer> getTriedServers() {
    return triedServers;
  }

  protected void setCurrentServer(ServerEntry currentServer) {
    this.currentServer = currentServer;
  }
}
