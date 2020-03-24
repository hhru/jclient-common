package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestEngineBuilder;
import ru.hh.jclient.common.RequestStrategy;

public class RequestBalancerBuilder implements RequestEngineBuilder<RequestBalancer> {

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
    return new RequestBalancer(request, upstreamManager, requestExecutor, maxTimeoutTries, forceIdempotence, adaptive, profile);
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
