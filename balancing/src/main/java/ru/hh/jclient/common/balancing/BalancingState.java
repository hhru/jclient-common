package ru.hh.jclient.common.balancing;

import java.util.ArrayList;
import java.util.List;

public class BalancingState {
  protected final Upstream upstream;
  protected final String profile;
  private final List<Integer> triedServers;
  private ServerEntry currentServer;

  public BalancingState(Upstream upstream, String profile) {
    this.upstream = upstream;
    this.profile = profile;
    this.triedServers = new ArrayList<>();
  }

  public UpstreamConfig getUpstreamConfig() {
    return upstream.getConfig(profile);
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

  private List<Integer> getTriedServers() {
    return triedServers;
  }

  protected void setCurrentServer(ServerEntry currentServer) {
    this.currentServer = currentServer;
  }
}
