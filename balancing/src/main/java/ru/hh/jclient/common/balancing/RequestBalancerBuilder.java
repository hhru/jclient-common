package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestEngineBuilder;
import ru.hh.jclient.common.RequestStrategy;

public class RequestBalancerBuilder implements RequestEngineBuilder<RequestBalancer> {

  private final UpstreamManager upstreamManager;
  private final HttpClient httpClient;

  public RequestBalancerBuilder(UpstreamManager upstreamManager, HttpClient httpClient) {
    this.upstreamManager = upstreamManager;
    this.httpClient = httpClient;
  }

  private Integer maxTimeoutTries;
  private boolean forceIdempotence;
  private boolean adaptive;
  private UpstreamProfileSelector upstreamProfileSelector;

  @Override
  public RequestBalancer build(Request request, RequestStrategy.RequestExecutor requestExecutor) {
    return new RequestBalancer(request, upstreamManager, requestExecutor, maxTimeoutTries, forceIdempotence, adaptive,
        upstreamProfileSelector != null ? upstreamProfileSelector
            : upstreamManager.getProfileSelector(httpClient.getContext())
    );
  }

  @Override
  public HttpClient backToClient() {
    return httpClient;
  }

  public RequestBalancerBuilder withMaxTimeoutTries(Integer maxTimeoutTries) {
    this.maxTimeoutTries = maxTimeoutTries;
    return this;
  }

  public RequestBalancerBuilder withForceIdempotence(boolean forceIdempotence) {
    this.forceIdempotence = forceIdempotence;
    return this;
  }

  public RequestBalancerBuilder withAdaptive(boolean adaptive) {
    this.adaptive = adaptive;
    return this;
  }

  public RequestBalancerBuilder withProfile(String profile) {
    this.upstreamProfileSelector = UpstreamProfileSelector.forProfile(profile);
    return this;
  }
}
