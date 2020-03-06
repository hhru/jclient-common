package ru.hh.jclient.common.balancing;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static java.util.Optional.ofNullable;

public class UpstreamGroup {
  static final String DEFAULT_PROFILE = "default";
  private final Map<String, Upstream> upstreamsByProfile;
  private final SortedMap<ProfileKey, String> profilesByMaxResponseTime;

  public UpstreamGroup(String profileName, Upstream upstream) {
    this.upstreamsByProfile = new ConcurrentHashMap<>();
    this.profilesByMaxResponseTime = new ConcurrentSkipListMap<>();
    var profileOrDefault = ofNullable(profileName).orElse(DEFAULT_PROFILE);
    this.upstreamsByProfile.put(profileOrDefault, upstream);
    this.profilesByMaxResponseTime.put(new ProfileKey(profileOrDefault, upstream.getConfig().getAllowedTimeoutMs()), profileOrDefault);
  }

  public String getFittingProfile(int timeoutToFit) {
    var boundary = new ProfileKey(null, timeoutToFit);
    return Optional.of(profilesByMaxResponseTime)
      .filter(Predicate.not(Map::isEmpty))
      .map(notEmptyMap -> getSlowestFittingOrFastest(notEmptyMap, boundary))
      .orElse(null);
  }

  private static String getSlowestFittingOrFastest(SortedMap<ProfileKey, String> profilesByResponseTime, ProfileKey boundary) {
    return Optional.of(profilesByResponseTime.headMap(boundary))
      .filter(Predicate.not(Map::isEmpty))
      .map(headMap -> headMap.get(headMap.lastKey()))
      .orElseGet(() -> profilesByResponseTime.get(profilesByResponseTime.firstKey()));
  }

  public Upstream getUpstreamOrDefault(@Nullable String profile) {
    if (profile != null) {
      return ofNullable(upstreamsByProfile.get(profile)).orElseGet(() -> upstreamsByProfile.get(DEFAULT_PROFILE));
    }
    return upstreamsByProfile.get(DEFAULT_PROFILE);
  }

  public boolean isEmpty() {
    return upstreamsByProfile.isEmpty();
  }

  public UpstreamGroup addOrUpdate(@Nullable String profileName, UpstreamConfig config,
                          BiFunction<String, UpstreamConfig, Upstream> upstreamFactory) {
    var profileOrDefault = ofNullable(profileName).orElse(DEFAULT_PROFILE);
    var updatedUpstream = upstreamsByProfile.compute(profileOrDefault, (name, existingUpstream) -> {
      if (existingUpstream != null) {
        existingUpstream.updateConfig(config);
        return existingUpstream;
      } else {
        return upstreamFactory.apply(name, config);
      }
    });
    // no atomicity - eventual consistency is ok
    profilesByMaxResponseTime.put(
        new ProfileKey(profileOrDefault, updatedUpstream.getConfig().getAllowedTimeoutMs()),
        profileOrDefault
    );
    return this;
  }

  public void remove(@Nullable String profileName) {
    var profileOrDefault = ofNullable(profileName).orElse(DEFAULT_PROFILE);
    ofNullable(upstreamsByProfile.remove(profileOrDefault))
        .map(upstream -> new ProfileKey(profileName, upstream.getConfig().getAllowedTimeoutMs()))
        .ifPresent(profilesByMaxResponseTime::remove);

  }

  private static final class ProfileKey implements Comparable<ProfileKey> {
    private static final Comparator<ProfileKey> CMP = Comparator.comparingInt(ProfileKey::getMaxPossibleTimeoutMs)
        .thenComparing(ProfileKey::getProfileName, Comparator.nullsLast(Comparator.naturalOrder()));
    private final String profileName;
    private final int maxPossibleTimeoutMs;

    private ProfileKey(String profileName, int maxPossibleTimeoutMs) {
      this.profileName = profileName;
      this.maxPossibleTimeoutMs = maxPossibleTimeoutMs;
    }

    public String getProfileName() {
      return profileName;
    }

    public int getMaxPossibleTimeoutMs() {
      return maxPossibleTimeoutMs;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      var thatKey = (ProfileKey) o;
      return maxPossibleTimeoutMs == thatKey.maxPossibleTimeoutMs && Objects.equals(profileName, thatKey.profileName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(profileName, maxPossibleTimeoutMs);
    }

    @Override
    public int compareTo(ProfileKey o) {
      return CMP.compare(this, o);
    }
  }
}
