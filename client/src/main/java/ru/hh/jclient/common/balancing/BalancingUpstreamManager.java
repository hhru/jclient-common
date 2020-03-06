package ru.hh.jclient.common.balancing;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpHeaderNames;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.UpstreamManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

public class BalancingUpstreamManager extends UpstreamManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingUpstreamManager.class);
  private static final String SCHEMA_SEPARATOR = "://";
  private static final int SCHEMA_SEPARATOR_LEN = 3;

  private final Map<String, UpstreamGroup> upstreams = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduledExecutor;
  private final Set<Monitoring> monitoring;
  private final String datacenter;
  private final boolean allowCrossDCRequests;
  private final Function<HttpClientContext, UpstreamProfileSelector> upstreamProfileSelectorProvider;

  public BalancingUpstreamManager(ScheduledExecutorService scheduledExecutor, Set<Monitoring> monitoring, String datacenter,
                                  boolean allowCrossDCRequests) {
    this(scheduledExecutor, monitoring, datacenter, allowCrossDCRequests, false);
  }

  public BalancingUpstreamManager(ScheduledExecutorService scheduledExecutor, Set<Monitoring> monitoring, String datacenter,
                                  boolean allowCrossDCRequests, boolean skipAdaptiveProfileSelection) {
    this(Map.of(), scheduledExecutor, monitoring, datacenter, allowCrossDCRequests, skipAdaptiveProfileSelection);
  }

  public BalancingUpstreamManager(Map<String, String> upstreamConfigs,
                                  ScheduledExecutorService scheduledExecutor,
                                  Set<Monitoring> monitoring,
                                  String datacenter,
                                  boolean allowCrossDCRequests) {
    this(upstreamConfigs, scheduledExecutor, monitoring, datacenter, allowCrossDCRequests, false);
  }

  public BalancingUpstreamManager(Map<String, String> upstreamConfigs,
                                  ScheduledExecutorService scheduledExecutor,
                                  Set<Monitoring> monitoring,
                                  String datacenter,
                                  boolean allowCrossDCRequests, boolean skipAdaptiveProfileSelection) {
    this.scheduledExecutor = requireNonNull(scheduledExecutor, "scheduledExecutor must not be null");
    this.monitoring = requireNonNull(monitoring, "monitorings must not be null");
    this.datacenter = datacenter;
    this.allowCrossDCRequests = allowCrossDCRequests;
    this.upstreamProfileSelectorProvider = skipAdaptiveProfileSelection ? ctx -> UpstreamProfileSelector.EMPTY
      : (HttpClientContext ctx) -> serviceName -> {
        var upstreamGroup = upstreams.get(serviceName);
        return ofNullable(ctx.getHeaders())
          .map(headers -> headers.get(HttpHeaderNames.X_OUTER_TIMEOUT_MS))
          .flatMap(values -> values.stream().findFirst())
          .map(Long::valueOf)
          .map(Duration::ofMillis)
          .map(outerTimeout -> {
            var alreadySpentTime = Duration.between(ctx.getRequestStart(), getNow());
            var expectedTimeout = outerTimeout.minus(alreadySpentTime);
            return upstreamGroup.getFittingProfile((int) expectedTimeout.toMillis());
          }).orElse(null);
      };

    requireNonNull(upstreamConfigs, "upstreamConfigs must not be null");
    upstreamConfigs.forEach(this::updateUpstream);
  }

  @Override
  public void updateUpstream(@Nonnull String upstreamName, String configString) {
    var upstreamKey = Upstream.UpstreamKey.ofComplexName(upstreamName);
    if (configString == null) {
      LOGGER.info("removing upstream: {}", upstreamName);
      upstreams.computeIfPresent(upstreamKey.getServiceName(), (key, upstreamGroup) -> {
        upstreamGroup.remove(upstreamKey.getProfileName());
        if (upstreamGroup.isEmpty()) {
          return null;
        }
        return upstreamGroup;
      });
      return;
    }
    var newConfig = UpstreamConfig.parse(configString);
    upstreams.compute(upstreamKey.getServiceName(), (key, existingGroup) -> {
      var upstreamGroup = ofNullable(existingGroup).orElseGet(UpstreamGroup::new);
      upstreamGroup.addOrUpdate(
          upstreamKey.getProfileName(), newConfig,
          (profileName, config) -> new Upstream(
              upstreamKey,
              newConfig,
              scheduledExecutor, datacenter, allowCrossDCRequests, true
          )
      );
      return upstreamGroup;
    });
  }

  @Override
  public Upstream getUpstream(String serviceName, @Nullable String profile) {
    return ofNullable(upstreams.get(getNameWithoutScheme(serviceName)))
        .map(group -> group.getUpstreamOrDefault(profile)).orElse(null);
  }

  @Nonnull
  @Override
  protected UpstreamProfileSelector getProfileSelector(HttpClientContext ctx) {
    return upstreamProfileSelectorProvider.apply(ctx);
  }

  protected LocalDateTime getNow() {
    return LocalDateTime.now();
  }

  @Override
  public Set<Monitoring> getMonitoring() {
    return Set.copyOf(monitoring);
  }

  static String getNameWithoutScheme(String host) {
    int beginIndex = host.indexOf(SCHEMA_SEPARATOR) + SCHEMA_SEPARATOR_LEN;
    return beginIndex > 2 ? host.substring(beginIndex) : host;
  }

  @VisibleForTesting
  Map<String, UpstreamGroup> getUpstreams() {
    return upstreams;
  }
}
