package ru.hh.jclient.common.balancing;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

public class UpstreamGroup {
  static final String DEFAULT_PROFILE = "default";
  private final Map<String, Upstream> upstreamsByProfile;
  private final Supplier<IllegalStateException> noDefaultProfileExSupplier;

  public UpstreamGroup(String serviceName, String profileName, Upstream upstream) {
    this.noDefaultProfileExSupplier = () -> new IllegalStateException("No " + DEFAULT_PROFILE + " profile for service <" + serviceName + '>');
    this.upstreamsByProfile = new ConcurrentHashMap<>();
    var profileOrDefault = ofNullable(profileName).orElse(DEFAULT_PROFILE);
    this.upstreamsByProfile.put(profileOrDefault, upstream);
  }

  public Upstream getUpstreamOrDefault(@Nullable String profile) {
    return ofNullable(profile).map(upstreamsByProfile::get).or(() -> ofNullable(upstreamsByProfile.get(DEFAULT_PROFILE)))
      .orElseThrow(noDefaultProfileExSupplier);
  }

  public int getMinServerSize() {
    return upstreamsByProfile.values().stream().mapToInt(Upstream::getServerCount).min()
      .orElseThrow(() -> new IllegalStateException("No upstreams in group"));
  }

  public boolean isEmpty() {
    return upstreamsByProfile.isEmpty();
  }

  public UpstreamGroup addOrUpdate(@Nullable String profileName, UpstreamConfig config,
                                   List<Server> servers, BiFunction<String, UpstreamConfig, Upstream> upstreamFactory) {
    var profileOrDefault = ofNullable(profileName).orElse(DEFAULT_PROFILE);
    upstreamsByProfile.compute(profileOrDefault, (name, existingUpstream) -> {
      if (existingUpstream != null) {
        existingUpstream.updateConfig(config, servers);
        return existingUpstream;
      } else {
        return upstreamFactory.apply(name, config);
      }
    });
    return this;
  }

  public void remove(@Nullable String profileName) {
    var profileOrDefault = ofNullable(profileName).orElse(DEFAULT_PROFILE);
    upstreamsByProfile.remove(profileOrDefault);
  }

}
