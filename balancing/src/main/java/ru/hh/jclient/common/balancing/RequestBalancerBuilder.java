package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestEngineBuilder;
import ru.hh.jclient.common.RequestStrategy;

import java.util.Optional;
import java.util.Set;

public class RequestBalancerBuilder implements RequestEngineBuilder {

  private final UpstreamManager upstreamManager;
  private final HttpClient httpClient;

  RequestBalancerBuilder(UpstreamManager upstreamManager, HttpClient httpClient) {
    this.upstreamManager = upstreamManager;
    this.httpClient = httpClient;
  }

  private Integer maxTimeoutTries;
  private boolean forceIdempotence;
  private boolean adaptive;
  private String profile;

  @Override
  public RequestBalancer build(Request request, RequestStrategy.RequestExecutor requestExecutor) {
    String host = request.getUri().getHost();
    Upstream upstream = upstreamManager.getUpstream(host, profile);
    double timeoutMultiplier = upstreamManager.getTimeoutMultiplier();
    Set<Monitoring> monitoring = upstreamManager.getMonitoring();
    if (upstream == null) {
      int maxTimeoutTries = Optional.ofNullable(this.maxTimeoutTries).orElse(UpstreamConfig.DEFAULT_MAX_TIMEOUT_TRIES);
      return new ExternalUrlRequestor(request, requestExecutor,
        requestExecutor.getDefaultRequestTimeoutMs(), maxTimeoutTries, UpstreamConfig.DEFAULT_MAX_TRIES,
        timeoutMultiplier, forceIdempotence, monitoring);
    } else {
      int maxTimeoutTries = Optional.ofNullable(this.maxTimeoutTries).orElseGet(() -> upstream.getConfig().getMaxTimeoutTries());
      BalancingState state;
      if (adaptive) {
        state = new AdaptiveBalancingState(upstream);
      } else {
        state = new BalancingState(upstream);
      }
      return new UpstreamRequestBalancer(state, request, requestExecutor,
        maxTimeoutTries, forceIdempotence, timeoutMultiplier, monitoring
      );
    }
  }

  @Override
  public HttpClient backToClient() {
    return httpClient;
  }

  public RequestBalancerBuilder withMaxTimeoutTries(int maxTimeoutTries) {
    this.maxTimeoutTries = maxTimeoutTries;
    return this;
  }

  public RequestBalancerBuilder forceIdempotence() {
    this.forceIdempotence = true;
    return this;
  }

  public RequestBalancerBuilder makeAdaptive() {
    this.adaptive = true;
    return this;
  }

  public RequestBalancerBuilder withProfile(String profile) {
    this.profile = profile;
    return this;
  }
}
