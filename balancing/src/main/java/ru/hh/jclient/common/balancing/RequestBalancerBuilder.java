package ru.hh.jclient.common.balancing;

import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestEngineBuilder;
import ru.hh.jclient.common.RequestStrategy;

public class RequestBalancerBuilder implements RequestEngineBuilder<RequestBalancerBuilder> {
  private static final Logger LOGGER = LoggerFactory.getLogger(RequestBalancerBuilder.class);

  private final UpstreamManager upstreamManager;
  private final HttpClient httpClient;

  RequestBalancerBuilder(UpstreamManager upstreamManager, HttpClient httpClient) {
    this.upstreamManager = upstreamManager;
    this.httpClient = httpClient;
  }

  protected String balancingRequestsLogLevel;
  private Double timeoutMultiplier;
  private Integer maxTimeoutTries;
  private Integer maxTries;
  private boolean forceIdempotence;
  private boolean adaptive;
  private String profile;
  private RetryPolicy retryPolicy;

  @Override
  public RequestBalancer build(Request request, RequestStrategy.RequestExecutor requestExecutor) {
    String host = request.getUri().getHost();
    Upstream upstream = upstreamManager.getUpstream(host, profile);
    Set<Monitoring> monitoring = upstreamManager.getMonitoring();

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("builderParams::: request: {}, profile: {}, upstream: {}, timeoutMultiplier: {}, " +
              "maxTimeoutTries: {}, maxTries: {}, forceIdempotence: {}, adaptive: {}",
          request, profile, upstream, timeoutMultiplier, maxTimeoutTries, maxTries, forceIdempotence, adaptive);
    }
    if (upstream == null || !upstream.isEnabled()) {
      int maxTimeoutTries = Optional.ofNullable(this.maxTimeoutTries).orElseGet(UpstreamConfig.DEFAULT_CONFIG::getMaxTimeoutTries);
      int maxTries = Optional.ofNullable(this.maxTries).orElseGet(UpstreamConfig.DEFAULT_CONFIG::getMaxTries);

      return new ExternalUrlRequestor(upstream, request, requestExecutor,
        requestExecutor.getDefaultRequestTimeoutMs(), maxTimeoutTries, maxTries,
        timeoutMultiplier, balancingRequestsLogLevel, forceIdempotence, monitoring, retryPolicy);
    } else {
      int maxTimeoutTries = Optional
          .ofNullable(this.maxTimeoutTries)
          .orElseGet(() -> upstream.getConfig(profile).getMaxTimeoutTries());
      BalancingState state;
      if (adaptive) {
        state = new AdaptiveBalancingState(upstream, profile);
      } else {
        state = new BalancingState(upstream, profile);
      }
      return new UpstreamRequestBalancer(state, request, requestExecutor,
        maxTimeoutTries, forceIdempotence, timeoutMultiplier, balancingRequestsLogLevel, monitoring
      );
    }
  }

  @Override
  public RequestBalancerBuilder withTimeoutMultiplier(Double timeoutMultiplier) {
    this.timeoutMultiplier = timeoutMultiplier;
    return this;
  }

  @Override
  public RequestBalancerBuilder withBalancingRequestsLogLevel(String balancingRequestsLogLevel) {
    this.balancingRequestsLogLevel = balancingRequestsLogLevel;
    return this;
  }

  @Override
  public HttpClient backToClient() {
    return httpClient;
  }

  public RequestBalancerBuilder withExternalMaxTimeoutTries(int maxTimeoutTries) {
    this.maxTimeoutTries = maxTimeoutTries;
    return this;
  }

  public RequestBalancerBuilder withExternalMaxTries(int maxTries) {
    this.maxTries = maxTries;
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

  public String getBalancingRequestsLogLevel() {
    return this.balancingRequestsLogLevel;
  }

  public RequestBalancerBuilder withExternalRetryPolicy(RetryPolicy retryPolicy) {
    this.retryPolicy = retryPolicy;
    return this;
  }
}
